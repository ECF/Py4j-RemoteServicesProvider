/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.ProcessDestroyer;
import org.apache.commons.exec.PumpStreamHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPythonLauncher implements PythonLauncher {

	public static final String PYTHON_LAUNCH_COMMAND_OPTION = "-c";

	protected static final Logger logger = LoggerFactory.getLogger(AbstractPythonLauncher.class);
	protected Object launchLock = new Object();
	protected boolean enabled = Boolean
			.valueOf(System.getProperty("org.eclipse.ecf.python.PythonLauncher.enabled", "true")).booleanValue();
	protected String pythonExec = System.getProperty("org.eclipse.ecf.python.PythonLauncher.pythonExec", "python");
	protected File pythonWorkingDirectory = Paths
			.get(System.getProperty("org.eclipse.ecf.python.PythonLauncher.workingDirectory", ".")).toFile();
	protected BundleContext context;
	protected Executor executor;
	protected boolean shuttingDown;

	protected Integer javaPort;
	protected Integer pythonPort;

	protected List<ServiceReference<PythonLaunchCommandProvider>> launchCommandProviders = Collections
			.synchronizedList(new ArrayList<ServiceReference<PythonLaunchCommandProvider>>());

	protected void bindLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		addPythonLaunchCommandProvider(provider);
	}

	protected void unbindPythonLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		removePythonLaunchCommandProvider(provider);
	}

	protected void addPythonLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		launchCommandProviders.add(provider);
	}

	protected void removePythonLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		launchCommandProviders.remove(provider);
	}

	protected void activate(BundleContext context) throws Exception {
		this.context = context;
	}

	protected void deactivate(BundleContext context) {
		this.context = null;
	}

	protected BundleContext getContext() {
		return this.context;
	}

	protected class PythonProcessDestroyer implements ProcessDestroyer {

		private List<Process> processes = Collections.synchronizedList(new ArrayList<Process>());

		@Override
		public boolean add(Process arg0) {
			return processes.add(arg0);
		}

		@Override
		public boolean remove(Process arg0) {
			return processes.remove(arg0);
		}

		@Override
		public int size() {
			return processes.size();
		}

		public void destroyAll() {
			for (Process p : processes)
				try {
					p.destroy();
				} catch (Exception e) {
					logger.error("Exception destroying process=" + p);
				}
		}
	}

	protected Executor createExecutor() {
		return new DefaultExecutor.Builder<>().get();
	}

	protected String createPythonLaunchCommand() {
		BundleContext context = getContext();
		if (context == null)
			return null;
		List<ServiceReference<PythonLaunchCommandProvider>> lcp = null;
		synchronized (this.launchCommandProviders) {
			lcp = new ArrayList<ServiceReference<PythonLaunchCommandProvider>>(this.launchCommandProviders);
		}
		// Sort by ranking
		Collections.sort(lcp, new Comparator<ServiceReference<PythonLaunchCommandProvider>>() {
			@Override
			public int compare(ServiceReference<PythonLaunchCommandProvider> o1,
					ServiceReference<PythonLaunchCommandProvider> o2) {
				Integer o1ranking = (Integer) o1.getProperty(Constants.SERVICE_RANKING);
				Integer o2ranking = (Integer) o2.getProperty(Constants.SERVICE_RANKING);
				return (o2ranking == null ? 0 : o2ranking.intValue()) - (o1ranking == null ? 0 : o1ranking.intValue());
			}
		});
		StringBuffer buf = new StringBuffer("");
		for (Iterator<ServiceReference<PythonLaunchCommandProvider>> i = lcp.iterator(); i.hasNext();) {
			ServiceReference<PythonLaunchCommandProvider> ref = i.next();
			String componentName = (String) ref.getProperty(ComponentConstants.COMPONENT_NAME);
			if (componentName != null)
				componentName = "PythonLaunchCommandProvider component name=" + componentName + "\n";
			else
				componentName = "PythonLaunchCommandProvider service id=" + ref.getProperty(Constants.SERVICE_ID)
						+ "\n";

			PythonLaunchCommandProvider plcp = context.getService(ref);
			buf.append("## Start of " + componentName);
			buf.append(plcp.getLaunchCommand());
			buf.append("## End of " + componentName);
			context.ungetService(ref);
		}
		return buf.toString();
	}

	@Override
	public void launch(String[] args, OutputStream output) throws Exception {
		synchronized (this.launchLock) {
			if (isLaunched())
				throw new IllegalStateException("Already started");

			this.shuttingDown = false;
			if (enabled) {
				String pythonLaunchCommand = createPythonLaunchCommand();
				if (pythonLaunchCommand == null)
					throw new NullPointerException("pythonLaunchCommand must not be null");

				logger.debug("pythonLaunchCommand=" + pythonLaunchCommand);

				this.executor = createExecutor();
				if (this.pythonWorkingDirectory != null)
					this.executor.setWorkingDirectory(pythonWorkingDirectory);

				if (output == null) {
					output = new LogOutputStream() {
						@Override
						protected void processLine(String line, int level) {
							logger.debug("PYTHON: " + line);
						}
					};
				}
				executor.setStreamHandler(new PumpStreamHandler(output));

				this.executor.setProcessDestroyer(new PythonProcessDestroyer());

				ExecuteResultHandler executeHandler = new DefaultExecuteResultHandler() {
					@Override
					public void onProcessComplete(int exitValue) {
						logger.debug("PYTHON EXIT=" + exitValue);
					}

					@Override
					public void onProcessFailed(ExecuteException e) {
						if (!shuttingDown)
							logger.debug("PYTHON EXCEPTION", e);
					}
				};

				CommandLine commandLine = new CommandLine(pythonExec).addArgument(PYTHON_LAUNCH_COMMAND_OPTION);
				commandLine.addArgument(pythonLaunchCommand, true);

				List<String> argsList = (args == null) ? Collections.emptyList() : Arrays.asList(args);

				if (this.javaPort != null && !argsList.contains(JAVA_PORT_OPTION)) {
					commandLine.addArgument(JAVA_PORT_OPTION);
					commandLine.addArgument(String.valueOf(this.javaPort));
				}

				if (this.pythonPort != null && !argsList.contains(PYTHON_PORT_OPTION)) {
					commandLine.addArgument(PYTHON_PORT_OPTION);
					commandLine.addArgument(String.valueOf(this.pythonPort));
				}

				if (args != null)
					commandLine.addArguments(args);
				logger.debug("PythonLauncher.launch: " + commandLine);
				try {
					executor.execute(commandLine, executeHandler);
				} catch (Exception e) {
					this.executor = null;
					throw e;
				}
			} else
				logger.debug("PythonLauncher DISABLED.   Python process must be started manually");
		}
	}

	@Override
	public void halt() {
		synchronized (this.launchLock) {
			if (!isLaunched())
				return;
			if (this.executor != null) {
				this.shuttingDown = true;
				PythonProcessDestroyer destroyer = (PythonProcessDestroyer) this.executor.getProcessDestroyer();
				if (destroyer != null)
					destroyer.destroyAll();
				this.executor = null;
			}
		}
	}

	@Override
	public boolean isLaunched() {
		synchronized (this.launchLock) {
			return this.executor != null;
		}
	}

}
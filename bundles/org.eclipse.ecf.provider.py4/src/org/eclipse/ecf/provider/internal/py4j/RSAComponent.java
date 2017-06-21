/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.internal.py4j;

import java.util.Hashtable;

import org.eclipse.ecf.provider.direct.CallableEndpoint;
import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.eclipse.ecf.provider.direct.local.ContainerExporterService;
import org.eclipse.ecf.provider.direct.local.ProxyMapperService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.py4j.osgi.GatewayServer;
import org.py4j.osgi.GatewayServerConfiguration;

import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

@Component(immediate = true)
public class RSAComponent {

	private static final boolean DEBUGPY4J = new Boolean(
			System.getProperty("org.eclipse.ecf.provider.py4j.debug", "false")).booleanValue();

	private BundleContext context;
	private ProxyMapperService javaConsumer;

	private static RSAComponent instance;

	private Py4JServerConnection connection;
	private ServiceRegistration<?> drspReg;

	private ContainerExporterService containerExporterService;

	private GatewayServer gateway;

	public static RSAComponent getDefault() {
		return instance;
	}

	public RSAComponent() {
		instance = this;
	}

	@Reference(service = ProxyMapperService.class)
	void bindLocalProxyMapper(ProxyMapperService pmm) {
		synchronized (this) {
			this.javaConsumer = pmm;
		}
	}

	void unbindLocalProxyMapper(ProxyMapperService pmm) {
		pmm.clear();
		synchronized (this) {
			this.javaConsumer = null;
		}
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	void bindContainerExporter(ContainerExporterService e) {
		this.containerExporterService = e;
	}

	void unbindContainerExporter(ContainerExporterService e) {
		this.containerExporterService = null;
	}

	public ContainerExporterService getContainerExporter() {
		return this.containerExporterService;
	}

	void logError(String message) {
		logError(message, null);
	}

	private void logError(String message, Throwable exception) {
		if (DEBUGPY4J) {
			System.err.println(message);
			if (exception != null)
				exception.printStackTrace(System.err);
		}
	}

	private GatewayServerListener gatewayServerListener = new GatewayServerListener() {

		@Override
		public void connectionError(Exception arg0) {
			logError("Connection error", arg0);
		}

		@Override
		public void connectionStarted(Py4JServerConnection arg0) {
			synchronized (RSAComponent.this) {
				if (RSAComponent.this.connection != null)
					logError("connectionStarted error: Already have connection=" + RSAComponent.this.connection
							+ ".  New connectionStarted=" + arg0);
				RSAComponent.this.connection = arg0;
			}
		}

		@Override
		public void connectionStopped(Py4JServerConnection arg0) {
			synchronized (RSAComponent.this) {
				if (RSAComponent.this.connection == null)
					logError("connectionStopped error: this.connection already null");
				else if (RSAComponent.this.connection != arg0)
					logError("connectionStopped error: this.connection=" + RSAComponent.this.connection
							+ " not equal to arg0=" + arg0);
				else {
					RSAComponent.this.connection = null;
					hardClose();
				}
			}
		}

		@Override
		public void serverError(Exception arg0) {
		}

		@Override
		public void serverPostShutdown() {
		}

		@Override
		public void serverPreShutdown() {
		}

		@Override
		public void serverStarted() {
		}

		@Override
		public void serverStopped() {
		}
	};

	public ProxyMapperService getProxyMapper() {
		return javaConsumer;
	}

	private void hardClose() {
		synchronized (this) {
			if (drspReg != null) {
				drspReg.unregister();
				drspReg = null;
			}
			if (javaConsumer != null)
				javaConsumer.clear();
		}
	}

	@SuppressWarnings("unchecked")
	public void setPythonConsumer(DirectRemoteServiceProvider consumer) {
		synchronized (this) {
			if (context != null && connection != null) {
				@SuppressWarnings("rawtypes")
				Hashtable ht = new Hashtable();
				ht.put(DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP,
						"python." + this.gateway.getConfiguration().getPythonPort());
				drspReg = context.registerService(
						new String[] { DirectRemoteServiceProvider.class.getName(), CallableEndpoint.class.getName() },
						consumer, ht);
			}
		}
	}

	public DirectRemoteServiceProvider getJavaConsumer() {
		return javaConsumer;
	}

	public String getGatewayHostAddress() {
		synchronized (this) {
			if (gateway == null)
				throw new NullPointerException("Gateway host cannot be accessed");
			return gateway.getConfiguration().getAddress().getHostAddress();
		}
	}

	public int getGatewayConnectTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getConnectTimeout();
		}
	}

	public int getGatewayReadTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getReadTimeout();
		}
	}

	public int getGatewayPort() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getListeningPort();
		}
	}

	@Activate
	void activate(BundleContext ctxt) throws Exception {
		context = ctxt;
		this.gateway = new GatewayServer(new GatewayServerConfiguration.Builder(this)
				.addGatewayServerListener(gatewayServerListener)
				.setClassLoadingStrategyBundles(new Bundle[] { context.getBundle() }).setDebug(DEBUGPY4J).build());
	}

	@Deactivate
	void deactivate() throws Exception {
		synchronized (this) {
			hardClose();
		}
		context = null;
	}

}

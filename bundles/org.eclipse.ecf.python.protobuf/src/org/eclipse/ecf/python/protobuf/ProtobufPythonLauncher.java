/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python.protobuf;

import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.python.AbstractPythonLauncher;
import org.eclipse.ecf.python.PythonLaunchCommandProvider;
import org.eclipse.ecf.python.PythonLauncher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ProtobufPythonLauncher extends AbstractPythonLauncher implements PythonLauncher {

	protected void bindPy4jProvider(Py4jProvider provider) {
		this.javaPort = provider.getJavaPort();
		this.pythonPort = provider.getPythonPort();
	}

	protected void unbindPy4jProvider(Py4jProvider provider) {
	}

	protected void bindLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		super.bindLaunchCommandProvider(provider);
	}

	protected void unbindPythonLaunchCommandProvider(ServiceReference<PythonLaunchCommandProvider> provider) {
		super.bindLaunchCommandProvider(provider);
	}

	protected void activate(BundleContext context) throws Exception {
		super.activate(context);
	}

	protected void deactivate() {
		halt();
		this.pythonWorkingDirectory = null;
	}

}

/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python.protobuf;

import java.io.IOException;

import org.eclipse.ecf.internal.python.protobuf.Activator;
import org.eclipse.ecf.python.AbstractPythonLaunchCommandProvider;
import org.eclipse.ecf.python.PythonLaunchCommandProvider;
import org.osgi.framework.BundleContext;

public class ProtobufPythonLaunchCommandProvider extends AbstractPythonLaunchCommandProvider
		implements PythonLaunchCommandProvider {

	public static final String DEFAULT_MAIN_PYTHON_COMMAND = "/python-src/__main__.py";

	protected void activate(BundleContext ctx) throws Exception {
		super.activate(ctx);
	}

	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String getLaunchCommand() {
		try {
			return getLaunchCommandFromBundleEntry(Activator.getDefault().getContext(), DEFAULT_MAIN_PYTHON_COMMAND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}

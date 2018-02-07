/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python;

import java.io.IOException;

import org.eclipse.ecf.provider.direct.BundleModuleResolver;
import org.osgi.framework.BundleContext;

public abstract class AbstractPythonLaunchCommandProvider implements PythonLaunchCommandProvider {

	protected BundleContext context;

	protected String getLaunchCommandFromBundleEntry(BundleContext context, String bundleEntry) throws IOException {
		return BundleModuleResolver.readFileAsUTF8(context.getBundle().getEntry(bundleEntry));
	}

	protected void activate(BundleContext context) throws Exception {
		this.context = context;
	}

	protected void deactivate() {
		this.context = null;
	}

}

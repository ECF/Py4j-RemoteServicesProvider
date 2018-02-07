/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.python.protobuf;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static Activator instance;
	private static BundleContext context;

	public static Activator getDefault() {
		return instance;
	}

	public BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext ctxt) throws Exception {
		instance = this;
		context = ctxt;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
		instance = null;
	}

}

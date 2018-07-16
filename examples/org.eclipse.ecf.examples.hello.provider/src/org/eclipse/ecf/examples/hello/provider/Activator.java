/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.hello.provider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ecf.provider.py4j.Py4jProviderActivatorImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private Py4jProviderActivatorImpl providerImpl;
	private boolean DEBUG = Boolean.valueOf(System.getProperty("org.eclipse.ecf.examples.hello.provider", "true"));

	@Override
	public void start(final BundleContext ctxt) throws Exception {
		Map<String, Object> props = new HashMap<String, Object>();
		if (DEBUG)
			props.put("debug", "true");
		this.providerImpl = new Py4jProviderActivatorImpl();
		this.providerImpl.open(ctxt, props);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (this.providerImpl != null) {
			this.providerImpl.close();
			this.providerImpl = null;
		}
	}

}

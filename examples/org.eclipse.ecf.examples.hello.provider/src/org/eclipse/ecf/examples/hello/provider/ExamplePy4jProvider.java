/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.hello.provider;

import java.util.Map;

import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.Py4jProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

@Component(immediate=true,enabled=false,property = {"debug=true", "port=25333", "pythonPort=25334" })
public class ExamplePy4jProvider extends Py4jProviderImpl implements Py4jProvider, RemoteServiceAdminListener {

	@Override
	@Reference(policy=ReferencePolicy.DYNAMIC)
	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}
	
	@Override
	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	protected void activate(BundleContext context, Py4jProviderImpl.Config config) throws Exception {
		super.activate(context,config);
	}
	
	@Activate
	protected void activate(BundleContext context, Map<String,?> properties) throws Exception {
		super.activate(context, properties);
	}
	
	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}
}

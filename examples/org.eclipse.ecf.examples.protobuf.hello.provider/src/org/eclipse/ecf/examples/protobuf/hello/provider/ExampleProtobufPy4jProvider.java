/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.protobuf.hello.provider;

import java.util.Map;

import org.eclipse.ecf.osgi.services.remoteserviceadmin.DebugRemoteServiceAdminListener;
import org.eclipse.ecf.provider.direct.BundleModuleResolver;
import org.eclipse.ecf.provider.direct.DirectProvider;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.protobuf.ProtobufPy4jProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

@Component(immediate = true, property = { "port=25333", "pythonPort=25334" })
public class ExampleProtobufPy4jProvider extends ProtobufPy4jProviderImpl
		implements RemoteServiceAdminListener, Py4jProvider, DirectProvider {

	@Reference(policy=ReferencePolicy.DYNAMIC,cardinality=ReferenceCardinality.MULTIPLE)
	protected void bindBundleModuleResolver(ServiceReference<BundleModuleResolver> ref) {
		super.bindBundleModuleResolver(ref);
	}
	
	protected void unbindBundleModuleResolver(ServiceReference<BundleModuleResolver> ref) {
		super.unbindBundleModuleResolver(ref);
	}
	
	@Reference
	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings({ "rawtypes" }) Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	@Override
	protected void activate(BundleContext context, Config config) throws Exception {
		super.activate(context, config);
	}

	private ServiceRegistration<?> debugRSAReg;
	
	@Activate
	protected void activate(BundleContext context, Map<String,?> properties) throws Exception {
		super.activate(context, properties);
		boolean debugRSA = Boolean.parseBoolean(System.getProperty("debugRSA","true"));
		if (debugRSA)
			debugRSAReg = getContext().registerService(RemoteServiceAdminListener.class, new DebugRemoteServiceAdminListener(), null);
	}
	
	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		if (debugRSAReg != null) {
			debugRSAReg.unregister();
			debugRSAReg = null;
		}
	}
}

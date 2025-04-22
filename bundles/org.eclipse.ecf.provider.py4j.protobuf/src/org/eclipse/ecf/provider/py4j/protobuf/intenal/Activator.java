/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.protobuf.intenal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.provider.direct.ModuleResolver;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.provider.py4j.protobuf.ProtobufPy4jProviderImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private ProtobufPy4jProviderImpl providerImpl;

	private static Logger logger = LoggerFactory.getLogger(Activator.class);
;
	private ServiceTracker<EndpointEventListener, EndpointEventListener> eelSt;
	private ServiceTracker<ModuleResolver, ModuleResolver> mrSt;

	private ServiceRegistration<?> providerReg;

	@Override
	public void start(final BundleContext context) throws Exception {
		Map<String, Object> props = new HashMap<String, Object>();
		if (logger.isDebugEnabled()) {
			props.put("debug", "true");
		} else {
			py4j.GatewayServer.turnLoggingOff();
			py4j.GatewayServer.PY4J_LOGGER.setLevel(Level.SEVERE);
		}
		this.providerImpl = new ProtobufPy4jProviderImpl();
		context.registerService(new String[] { Namespace.class.getName() }, new Py4jNamespace(), null);
		this.eelSt = new ServiceTracker<EndpointEventListener, EndpointEventListener>(context,
				EndpointEventListener.class,
				new ServiceTrackerCustomizer<EndpointEventListener, EndpointEventListener>() {
					@Override
					public EndpointEventListener addingService(ServiceReference<EndpointEventListener> reference) {
						EndpointEventListener eel = context.getService(reference);
						providerImpl.bindEndpointEventListener(eel,
								ProtobufPy4jProviderImpl.getPropsFromReference(reference));
						return eel;
					}

					@Override
					public void modifiedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {
					}

					@Override
					public void removedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {
						providerImpl.unbindEndpointEventListener(service);
					}
				});
		eelSt.open();
		this.mrSt = new ServiceTracker<ModuleResolver, ModuleResolver>(context, ModuleResolver.class,
				new ServiceTrackerCustomizer<ModuleResolver, ModuleResolver>() {

					@Override
					public ModuleResolver addingService(ServiceReference<ModuleResolver> reference) {
						ModuleResolver mr = context.getService(reference);
						providerImpl.bindModuleResolver(reference);
						return mr;
					}

					@Override
					public void modifiedService(ServiceReference<ModuleResolver> reference, ModuleResolver service) {
					}

					@Override
					public void removedService(ServiceReference<ModuleResolver> reference, ModuleResolver service) {
						providerImpl.unbindModuleResolver(reference);
					}
				});
		mrSt.open();
		// activating the providerImpl will setup java listener
		providerImpl.activate(context, props);
		this.providerReg = context.registerService(
				new String[] { Py4jProvider.class.getName(), RemoteServiceAdminListener.class.getName() }, providerImpl, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (this.providerReg != null) {
			this.providerReg.unregister();
			this.providerReg = null;
		}
		if (eelSt != null) {
			eelSt.close();
			eelSt = null;
		}
		if (mrSt != null) {
			mrSt.close();
			mrSt = null;
		}
		providerImpl.deactivate();
	}

}

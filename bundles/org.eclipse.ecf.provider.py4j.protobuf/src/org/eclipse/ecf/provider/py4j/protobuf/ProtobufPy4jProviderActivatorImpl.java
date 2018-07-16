/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.protobuf;

import java.util.Map;

import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.provider.direct.ModuleResolver;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.provider.py4j.protobuf.ProtobufPy4jProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ProtobufPy4jProviderActivatorImpl extends ProtobufPy4jProviderImpl {

	private ServiceTracker<EndpointEventListener, EndpointEventListener> eelSt;
	private ServiceTracker<ModuleResolver,ModuleResolver> mrSt;
	
	private ServiceRegistration<?> providerReg;

	@SuppressWarnings("unchecked")
	public void open(final BundleContext context, @SuppressWarnings("rawtypes") Map props) throws Exception {
		context.registerService(new String[] { Namespace.class.getName() }, new Py4jNamespace(), null);
		this.eelSt = new ServiceTracker<EndpointEventListener, EndpointEventListener>(context, EndpointEventListener.class,
				new ServiceTrackerCustomizer<EndpointEventListener, EndpointEventListener>() {
					@Override
					public EndpointEventListener addingService(ServiceReference<EndpointEventListener> reference) {
						EndpointEventListener eel = context.getService(reference);
						ProtobufPy4jProviderActivatorImpl.super.bindEndpointEventListener(eel, getPropsFromReference(reference));
						return eel;
					}
					@Override
					public void modifiedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {}
					@Override
					public void removedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {
						ProtobufPy4jProviderActivatorImpl.super.unbindEndpointEventListener(service);
					}
				});
		eelSt.open();
		this.mrSt = new ServiceTracker<ModuleResolver, ModuleResolver>(context, ModuleResolver.class,
				new ServiceTrackerCustomizer<ModuleResolver, ModuleResolver>() {

					@Override
					public ModuleResolver addingService(ServiceReference<ModuleResolver> reference) {
						ModuleResolver mr = context.getService(reference);
						ProtobufPy4jProviderActivatorImpl.super.bindModuleResolver(reference);
						return mr;
					}
					@Override
					public void modifiedService(ServiceReference<ModuleResolver> reference,
							ModuleResolver service) {}
					@Override
					public void removedService(ServiceReference<ModuleResolver> reference,
							ModuleResolver service) {
						ProtobufPy4jProviderActivatorImpl.super.unbindModuleResolver(reference);
					}
				});
		mrSt.open();
		super.activate(context, props);
		this.providerReg = context.registerService(
				new String[] { Py4jProvider.class.getName(), RemoteServiceAdminListener.class.getName() }, this, null);
	}

	public void close() {
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
		super.deactivate();
	}
}
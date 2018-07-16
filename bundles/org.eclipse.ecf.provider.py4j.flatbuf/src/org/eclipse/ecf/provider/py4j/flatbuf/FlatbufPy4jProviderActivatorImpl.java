/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.flatbuf;

import java.util.Map;

import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class FlatbufPy4jProviderActivatorImpl extends FlatbufPy4jProviderImpl {

	private ServiceTracker<EndpointEventListener, EndpointEventListener> eelSt;
	private ServiceRegistration<?> providerReg;

	@SuppressWarnings("unchecked")
	public void open(final BundleContext context, @SuppressWarnings("rawtypes") Map props) throws Exception {
		context.registerService(new String[] { Namespace.class.getName() }, new Py4jNamespace(), null);
		this.eelSt = new ServiceTracker<EndpointEventListener, EndpointEventListener>(context, EndpointEventListener.class,
				new ServiceTrackerCustomizer<EndpointEventListener, EndpointEventListener>() {
					@Override
					public EndpointEventListener addingService(ServiceReference<EndpointEventListener> reference) {
						EndpointEventListener eel = context.getService(reference);
						FlatbufPy4jProviderActivatorImpl.super.bindEndpointEventListener(eel, getPropsFromReference(reference));
						return eel;
					}
					@Override
					public void modifiedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {}
					@Override
					public void removedService(ServiceReference<EndpointEventListener> reference,
							EndpointEventListener service) {
						FlatbufPy4jProviderActivatorImpl.super.unbindEndpointEventListener(service);
					}
				});
		eelSt.open();
		super.activate(context, props);
		providerReg = context.registerService(
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
		super.deactivate();
	}
}
/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.internal.protobuf;

import java.util.Map;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpoint;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.provider.py4j.protobuf.Py4jProtobufConstants;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static BundleContext context;
	private static Activator instance;
	
	static BundleContext getContext() {
		return context;
	}

	private static final String[] py4jSupportedIntents = { "passByValue", "exactlyOnce", "ordered" };

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.instance = this;
		Activator.context = bundleContext;
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(Py4jProtobufConstants.ECF_PY4J_CONSUMER_PB)
						.setInstantiator(new RemoteServiceContainerInstantiator(Py4jProtobufConstants.ECF_PY4J_HOST_PYTHON_PB,
								Py4jProtobufConstants.ECF_PY4J_CONSUMER_PB) {
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								return new org.eclipse.ecf.provider.py4j.protobuf.Py4jProtobufClientContainer(Py4jNamespace.createUUID());
							}

							public String[] getSupportedIntents(ContainerTypeDescription description) {
								return py4jSupportedIntents;
							}
						}).setServer(false).setHidden(false).build(),
				null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		Activator.instance = null;
	}

	public static Activator getDefault() {
		return instance;
	}
	
	public ProtobufCallableEndpoint getCallableEndpoint() {
		if (context == null) return null;
		ServiceTracker<ProtobufCallableEndpoint,ProtobufCallableEndpoint> st = new ServiceTracker<ProtobufCallableEndpoint,ProtobufCallableEndpoint>(context,ProtobufCallableEndpoint.class,null);
		st.open();
		ProtobufCallableEndpoint result = st.getService();
		st.close();
		return result;
	}
}

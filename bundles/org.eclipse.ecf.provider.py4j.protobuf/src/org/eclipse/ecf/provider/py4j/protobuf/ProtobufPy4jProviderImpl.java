/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.protobuf;

import java.util.Map;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.DirectProvider;
import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpointImpl;
import org.eclipse.ecf.provider.direct.util.DirectHostContainer;
import org.eclipse.ecf.provider.direct.util.DirectRemoteServiceClientDistributionProvider;
import org.eclipse.ecf.provider.direct.util.DirectRemoteServiceHostDistributionProvider;
import org.eclipse.ecf.provider.direct.util.IDirectContainerInstantiator;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.Py4jProviderImpl;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

import com.google.protobuf.Message;

/**
 * Implementation of Protobuf-py4j remote service distribution provider.
 * 
 * @author slewis
 *
 */
public class ProtobufPy4jProviderImpl extends Py4jProviderImpl
		implements RemoteServiceAdminListener, Py4jProvider, DirectProvider {

	protected static final String[] py4jProtobufSupportedIntents = { "passByValue", "exactlyOnce", "ordered", "py4j",
			"py4j.protobuf", "py4j.async", "osgi.async", "osgi.basic", "osgi.private", "osgi.confidential" };

	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	protected ServiceRegistration<?> protobufHostReg = null;
	protected ServiceRegistration<?> protobufClientReg = null;

	protected void registerProtobufHostDistributionProvider() {
		protobufHostReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceHostDistributionProvider(ProtobufPy4jConstants.JAVA_HOST_CONFIG_TYPE,
						ProtobufPy4jConstants.PYTHON_CONSUMER_CONFIG_TYPE, new IDirectContainerInstantiator() {
							@Override
							public IContainer createContainer() throws ContainerCreateException {
								ID lId = ProtobufPy4jProviderImpl.this.localId;
								if (lId == null)
									throw new ContainerCreateException("Cannot create container with null localId");
								return new DirectHostContainer(lId, getInternalServiceProvider());
							}
						}, py4jProtobufSupportedIntents),
				null);
	}

	protected void registerProtobufClientDistributionProvider() {
		protobufClientReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceClientDistributionProvider(ProtobufPy4jConstants.JAVA_CONSUMER_CONFIG_TYPE,
						ProtobufPy4jConstants.PYTHON_HOST_CONFIG_TYPE, new IDirectContainerInstantiator() {
							@Override
							public IContainer createContainer() throws ContainerCreateException {
								return new org.eclipse.ecf.provider.direct.protobuf.ProtobufClientContainer(
										Py4jNamespace.createUUID(), new ProtobufCallableEndpoint() {
											@Override
											public <A extends Message> Message call_endpoint(Long rsId,
													String methodName, A message) throws Exception {
												return getProtobufCallableEndpoint().call_endpoint(rsId, methodName,
														message);
											}
										});
							}
						}, py4jProtobufSupportedIntents),
				null);
	}

	protected void activate(BundleContext context, Py4jProviderImpl.Config config) throws Exception {
		synchronized (getLock()) {
			super.activate(context, config);
			registerProtobufHostDistributionProvider();
			registerProtobufClientDistributionProvider();
		}
	}

	protected void activate(BundleContext context, Map<String, ?> properties) throws Exception {
		synchronized (getLock()) {
			super.activate(context, properties);
			registerProtobufHostDistributionProvider();
			registerProtobufClientDistributionProvider();
		}
	}

	private ProtobufCallableEndpointImpl pcei;

	protected ProtobufCallableEndpoint getProtobufCallableEndpoint() {
		ProtobufCallableEndpoint pce = null;
		synchronized (getLock()) {
			pce = this.pcei;
		}
		if (pce == null)
			throw new NullPointerException("Protobuf callable endpoint is null");
		return pce;
	}

	@Override
	protected void bindExternalCallableEndpoint(ExternalCallableEndpoint ep) {
		synchronized (getLock()) {
			super.bindExternalCallableEndpoint(ep);
			this.pcei = new ProtobufCallableEndpointImpl();
			this.pcei.bindExternalCallableEndpoint(ep);
		}
	}

	@Override
	protected void unbindExternalCallableEndpoint() {
		synchronized (getLock()) {
			super.unbindExternalCallableEndpoint();
			if (pcei != null) {
				pcei.unbindExternalCallableEndpoint();
				pcei = null;
			}
		}
	}

	protected void deactivate() {
		synchronized (getLock()) {
			super.deactivate();
			if (protobufClientReg != null) {
				protobufClientReg.unregister();
				protobufClientReg = null;
			}
			if (protobufHostReg != null) {
				protobufHostReg.unregister();
				protobufHostReg = null;
			}
		}
	}

}

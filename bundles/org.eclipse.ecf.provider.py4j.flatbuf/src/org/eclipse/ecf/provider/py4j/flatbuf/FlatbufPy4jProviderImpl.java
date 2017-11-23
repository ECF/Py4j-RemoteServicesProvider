/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.flatbuf;

import java.util.Map;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.provider.direct.DirectProvider;
import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.eclipse.ecf.provider.direct.flatbuf.FlatbufCallableEndpoint;
import org.eclipse.ecf.provider.direct.flatbuf.FlatbufCallableEndpointImpl;
import org.eclipse.ecf.provider.direct.util.DirectRemoteServiceClientDistributionProvider;
import org.eclipse.ecf.provider.direct.util.IDirectContainerInstantiator;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.eclipse.ecf.provider.py4j.Py4jProviderImpl;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

/**
 * Implementation of Protobuf-py4j remote service distribution provider.
 * 
 * @author slewis
 *
 */
public class FlatbufPy4jProviderImpl extends Py4jProviderImpl
		implements RemoteServiceAdminListener, Py4jProvider, DirectProvider {

	protected static final String[] py4jFlatbufSupportedIntents = { "passByValue", "exactlyOnce", "ordered" };

	@Reference
	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	protected ServiceRegistration<?> flatbufClientReg = null;

	protected void registerProtobufClientDistributionProvider() {
		flatbufClientReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceClientDistributionProvider(FlatbufPy4jConstants.ECF_PY4J_CONSUMER_FB,
						FlatbufPy4jConstants.ECF_PY4J_HOST_PYTHON_FB, new IDirectContainerInstantiator() {
							@Override
							public IContainer createContainer() throws ContainerCreateException {
								return new org.eclipse.ecf.provider.direct.flatbuf.FlatbufClientContainer(
										Py4jNamespace.createUUID(), new FlatbufCallableEndpoint() {
											@Override
											public Table call_endpoint(Long rsId, String methodName, FlatBufferBuilder builder,
													Class<?> resultType) throws Exception {
												return getFlatbufCallableEndpoint().call_endpoint(rsId, methodName,
														builder, resultType);
											}
										});
							}
						}, py4jFlatbufSupportedIntents),
				null);
	}

	protected void activate(BundleContext context, Py4jProviderImpl.Config config) throws Exception {
		synchronized (getLock()) {
			super.activate(context, config);
			registerProtobufClientDistributionProvider();
		}
	}

	protected void activate(BundleContext context, Map<String, ?> properties) throws Exception {
		synchronized (getLock()) {
			super.activate(context, properties);
			registerProtobufClientDistributionProvider();
		}
	}

	private FlatbufCallableEndpointImpl pcei;

	protected FlatbufCallableEndpoint getFlatbufCallableEndpoint() {
		FlatbufCallableEndpoint pce = null;
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
			this.pcei = new FlatbufCallableEndpointImpl();
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
			if (flatbufClientReg != null) {
				flatbufClientReg.unregister();
				flatbufClientReg = null;
			}
		}
	}

}

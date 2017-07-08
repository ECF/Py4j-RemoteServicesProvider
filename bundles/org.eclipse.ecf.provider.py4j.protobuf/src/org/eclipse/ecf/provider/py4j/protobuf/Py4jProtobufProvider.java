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

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpointImpl;
import org.eclipse.ecf.provider.py4j.IPy4jDirectProvider;
import org.eclipse.ecf.provider.py4j.Py4jDirectProvider;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

@Component(immediate = true)
public class Py4jProtobufProvider extends Py4jDirectProvider
		implements RemoteServiceAdminListener, IPy4jDirectProvider {

	protected static final String[] py4jProtobufSupportedIntents = { "passByValue", "exactlyOnce", "ordered" };

	@Reference
	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	protected ServiceRegistration<?> protobufClientReg = null;

	protected void registerProtobufClientDistributionProvider() {
		protobufClientReg = getContext()
				.registerService(IRemoteServiceDistributionProvider.class,
						new RemoteServiceDistributionProvider.Builder()
								.setName(Py4jProtobufConstants.ECF_PY4J_CONSUMER_PB)
								.setInstantiator(new RemoteServiceContainerInstantiator(
										Py4jProtobufConstants.ECF_PY4J_HOST_PYTHON_PB,
										Py4jProtobufConstants.ECF_PY4J_CONSUMER_PB) {
									public IContainer createInstance(ContainerTypeDescription description,
											Map<String, ?> parameters) {
										return new org.eclipse.ecf.provider.py4j.protobuf.Py4jProtobufClientContainer(
												Py4jNamespace.createUUID(), new ProviderProtobufCallableEndpoint());
									}
									public String[] getSupportedIntents(ContainerTypeDescription description) {
										return py4jProtobufSupportedIntents;
									}
								}).setServer(false).setHidden(false).build(),
						null);
	}

	protected class ProviderProtobufCallableEndpoint implements ProtobufCallableEndpoint {
		@Override
		public <A extends Message> Message call_endpoint(Long rsId, String methodName, A message,
				Parser<?> resultParser) throws Exception {
			return getProtobufCallableEndpoint().call_endpoint(rsId, methodName, message, resultParser);
		}
	}

	@Activate
	protected void activate(BundleContext context, Map<String, ?> properties) throws Exception {
		synchronized (getLock()) {
			super.activate(context, properties);
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

	@Deactivate
	protected void deactivate() {
		synchronized (getLock()) {
			super.deactivate();
			if (protobufClientReg != null) {
				protobufClientReg.unregister();
				protobufClientReg = null;
			}
		}
	}

}

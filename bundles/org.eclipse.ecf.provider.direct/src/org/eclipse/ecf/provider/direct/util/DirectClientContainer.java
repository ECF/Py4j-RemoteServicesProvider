/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.util;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.ExternalServiceProvider;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.ecf.remoteservice.events.IRemoteCallCompleteEvent;

/**
 * Implementation of ECF client container for direct distribution providers.
 * 
 * @author slewis
 *
 */
public class DirectClientContainer extends AbstractRSAClientContainer {

	private ExternalServiceProvider externalServiceProvider;

	public DirectClientContainer(ID containerID, ExternalServiceProvider spp) {
		super(containerID);
		this.externalServiceProvider = spp;
	}

	protected ExternalServiceProvider getServiceProxyProvider() {
		return externalServiceProvider;
	}

	@Override
	protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration) {
		return new DirectRSAClientService(this, registration);
	}

	protected Object getProxy(RemoteServiceClientRegistration reg) throws InvalidObjectException {
		String proxyId = (String) reg.getProperty(ExternalServiceProvider.PROXYID_PROP_NAME);
		if (proxyId == null)
			throw new InvalidObjectException("Could not get proxyId for remote service=" + reg.getID());
		Object proxy = externalServiceProvider.getProxy(proxyId);
		if (proxy == null)
			throw new InvalidObjectException(
					"Could not get proxy for proxyId=" + proxyId + " on remote service=" + reg.getID());
		return proxy;
	}

	public class DirectRSAClientService extends AbstractRSAClientService {

		public DirectRSAClientService(AbstractClientContainer container, RemoteServiceClientRegistration registration) {
			super(container, registration);
		}

		@SuppressWarnings("rawtypes")
		@Override
		protected Callable<IRemoteCallCompleteEvent> getAsyncCallable(final RSARemoteCall call) {
			return () -> {
				Object result = invokeMethod(call.getReflectMethod(), call.getParameters());
				return createRCCESuccess(((CompletableFuture) result).get());
			};
		}
		
		@Override
		protected Callable<Object> getSyncCallable(final RSARemoteCall call) {
			return () -> {
				return invokeMethod(call.getReflectMethod(),call.getParameters());
			};
		}
		
		private Object invokeMethod(Method m, Object[] parameters) throws Exception {
			Object proxy = ((DirectClientContainer) container).getProxy(registration);
			return m.invoke(proxy, parameters);
		}
	}

}

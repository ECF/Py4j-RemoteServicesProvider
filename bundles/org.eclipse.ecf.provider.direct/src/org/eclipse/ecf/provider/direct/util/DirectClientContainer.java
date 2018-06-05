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

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.ExternalServiceProvider;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.asyncproxy.AsyncReturnUtil;
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
		// See DirectRSAClientService class below
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

		@Override
		protected Callable<IRemoteCallCompleteEvent> getAsyncCallable(final RSARemoteCall call) {
			return () -> {
				Method reflectMethod = call.getReflectMethod();
				// Invoke selected method on proxy.  This will make the actual
				// remote call
				Object result = invokeMethodOnProxy(reflectMethod, call.getParameters());
				// Get return type from reflectMethod
				Class<?> returnClass = reflectMethod.getReturnType();
				// The return type should be one of the async types and we use the 
				// AsyncReturnUtil to convert from the async type to the simple type (i.e. the type returned)
				// by the async result
				Object simpleResult = AsyncReturnUtil.convertAsyncToReturn(result,returnClass,call.getTimeout());
				// And return a success event
				return createRCCESuccess(simpleResult);
			};
		}
		
		@Override
		protected Callable<Object> getSyncCallable(final RSARemoteCall call) {
			return () -> {
				// Synchronously invoke the call method on the proxy
				return invokeMethodOnProxy(call.getReflectMethod(),call.getParameters());
			};
		}
		
		private Object invokeMethodOnProxy(Method m, Object[] parameters) throws Exception {
			// Get the proxy for this registration and container
			Object proxy = ((DirectClientContainer) container).getProxy(registration);
			// Invoke the method on it with given parameters
			return m.invoke(proxy, parameters);
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.io.InvalidObjectException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.equinox.concurrent.future.IExecutor;
import org.eclipse.equinox.concurrent.future.IProgressRunnable;

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

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Object invokeAsync(final RSARemoteCall remoteCall) throws ECFException {
			final CompletableFuture cf = new CompletableFuture();
			IExecutor executor = getIFutureExecutor(remoteCall);
			if (executor == null)
				throw new ECFException("no executor available to invoke asynchronously");
			executor.execute(new IProgressRunnable() {
				@Override
				public Object run(IProgressMonitor arg0) throws Exception {
					try {
						Object[] params = remoteCall.getParameters();
						Class[] paramTypes = (params == null) ? new Class[0] : new Class[params.length];
						if (params != null)
							for (int i = 0; i < params.length; i++)
								paramTypes[i] = params[i].getClass();
						cf.complete(invokeMethod(getProxy().getClass().getMethod(remoteCall.getMethod(), paramTypes),
								params));
					} catch (Throwable e) {
						cf.completeExceptionally(e);
					}
					return null;
				}
			}, null);
			return cf;
		}

		private Object invokeMethod(Method m, Object[] parameters) throws ECFException {
			try {
				return m.invoke(getProxy(), parameters);
			} catch (Throwable e) {
				throw new ECFException("Cannot invoke remoteCall on proxy", e);
			}
		}

		@Override
		protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
			return invokeMethod(remoteCall.getReflectMethod(), remoteCall.getParameters());
		}
	}

}

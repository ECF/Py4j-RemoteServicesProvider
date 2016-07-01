/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.local;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.Assert;
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

public class RSAClientContainer extends AbstractRSAClientContainer {

	private ProxyMapperService mapper;
	private Object proxy;

	public RSAClientContainer(ID containerID, Object proxy) {
		super(containerID);
		Assert.isNotNull(proxy);
		this.proxy = proxy;
	}

	public RSAClientContainer(ID containerID, ProxyMapperService mapper) {
		super(containerID);
		Assert.isNotNull(mapper);
		this.mapper = mapper;
	}

	protected Object getProxy() {
		return this.proxy;
	}

	protected ProxyMapperService getProxyMapper() {
		return this.mapper;
	}

	@Override
	protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration) {
		synchronized (this) {
			if (this.proxy == null)
				this.proxy = this.mapper.getProxy(registration.getID().getContainerRelativeID());
		}
		return new DirectRSAClientService(this, registration, this.proxy);
	}

	public class DirectRSAClientService extends AbstractRSAClientService {

		private final Object proxy;

		public DirectRSAClientService(AbstractClientContainer container, RemoteServiceClientRegistration registration,
				Object proxy) {
			super(container, registration);
			Assert.isNotNull(proxy);
			this.proxy = proxy;
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
						cf.complete(
								invokeMethod(proxy.getClass().getMethod(remoteCall.getMethod(), paramTypes), params));
					} catch (Exception e) {
						cf.completeExceptionally(e);
					}
					return null;
				}
			}, null);
			return cf;
		}

		private Object invokeMethod(Method m, Object[] parameters) throws Exception {
			try {
				return m.invoke(proxy, parameters);
			} catch (Exception e) {
				throw new ECFException("Cannot invoke remoteCall on proxy", e);
			}
		}

		@Override
		protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
			try {
				return invokeMethod(remoteCall.getReflectMethod(), remoteCall.getParameters());
			} catch (Exception e) {
				throw new ECFException("Cannot invoke method on proxy", e);
			}
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.flatbuf;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.remoteservice.IAsyncRemoteServiceProxy;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.equinox.concurrent.future.IExecutor;
import org.eclipse.equinox.concurrent.future.IFuture;
import org.eclipse.equinox.concurrent.future.IProgressRunnable;

import com.google.flatbuffers.Table;

public class FlatbufClientContainer extends AbstractRSAClientContainer {

	private final FlatbufCallableEndpoint endpoint;

	public FlatbufClientContainer(ID containerID, FlatbufCallableEndpoint endpoint) {
		super(containerID);
		this.endpoint = endpoint;
	}

	protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration) {
		return new FlatBufDirectRemoteService(this, registration);
	}

	public class FlatBufDirectRemoteService extends AbstractRSAClientService {

		public static final String PROTOBUF_PARSER_STATIC_METHODNAME = "parser";

		private Long rsId;

		public FlatBufDirectRemoteService(AbstractClientContainer container,
				RemoteServiceClientRegistration registration) {
			super(container, registration);
			this.rsId = registration.getID().getContainerRelativeID();
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected Object invokeAsync(final RSARemoteCall remoteCall) throws ECFException {
			final CompletableFuture cf = new CompletableFuture();
			IExecutor executor = getIFutureExecutor(remoteCall);
			if (executor == null)
				throw new ECFException("no executor available to invoke asynchronously");
			executor.execute(new IProgressRunnable() {
				@Override
				public Object run(IProgressMonitor arg0) throws Exception {
					try {
						cf.complete(invokeSync(remoteCall));
					} catch (Throwable e) {
						cf.completeExceptionally(e);
					}
					return null;
				}
			}, null);
			return cf;
		}

		private Class<?> getSyncReturnType(Object proxy, Method m) throws ECFException {
			Class<?> asyncClass = m.getDeclaringClass();
			String asyncClassName = asyncClass.getName();
			String syncClassName = asyncClassName.substring(0,
					asyncClassName.length() - IAsyncRemoteServiceProxy.ASYNC_INTERFACE_SUFFIX.length());
			// Now look for/find class with syncClassName on proxy
			Class<?> syncClass = findSyncClass(proxy, syncClassName);
			if (syncClass != null) {
				String asyncMethodName = m.getName();
				String syncMethodName = asyncMethodName.substring(0,
						asyncMethodName.length() - IAsyncRemoteServiceProxy.ASYNC_METHOD_SUFFIX.length());
				try {
					return syncClass.getMethod(syncMethodName, m.getParameterTypes()).getReturnType();
				} catch (NoSuchMethodException e) {
					throw new ECFException(
							"No sync method=" + syncMethodName + " found on class=" + syncClass.getName());
				}
			}
			throw new ECFException(
					"No syncClass found for async class=" + asyncClass.getName() + " for method=" + m.getName());
		}

		private Class<?> findSyncClass(Object proxy, String syncClassName) {
			Class<?>[] intfs = proxy.getClass().getInterfaces();
			for (Class<?> intf : intfs)
				if (intf.getName().equals(syncClassName))
					return intf;
			return null;
		}

		@Override
		protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
			// Checks on arguments/parameters
			Object[] args = remoteCall.getParameters();
			Object message = null;
			if (args != null && args.length > 0) {
				message = args[0];
				if (message != null && !(message instanceof Table))
					throw new ECFException("Remote call=" + remoteCall + " the first parameter must be of type Table");
			}
			Method reflectMethod = remoteCall.getReflectMethod();
			Class<?> returnType = reflectMethod.getReturnType();
			// If it's a CompletableFuture then replace return type with return type of sync
			// method
			if (returnType.equals(CompletableFuture.class) || returnType.equals(IFuture.class)
					|| returnType.equals(Future.class))
				returnType = getSyncReturnType(remoteCall.getProxy(), reflectMethod);

			if (returnType == null || returnType.equals(Void.TYPE)) {
				returnType = Void.TYPE;
			} else if (!Table.class.isAssignableFrom(returnType))
				throw new ECFException("Remote call=" + remoteCall + " return type is not Message");

			try {
				// Actually make call via AbstractProtobufCallableEndpoint
				return endpoint.call_endpoint(rsId, remoteCall.getMethod(), (Table) message, returnType);
			} catch (Exception e) {
				throw new ECFException("Could not execute remote call=" + remoteCall, e);
			}
		}
	}
}

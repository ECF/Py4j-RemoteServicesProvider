/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.protobuf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpoint;
import org.eclipse.ecf.provider.py4j.internal.protobuf.Activator;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.equinox.concurrent.future.IExecutor;
import org.eclipse.equinox.concurrent.future.IProgressRunnable;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class Py4jProtobufClientContainer extends AbstractRSAClientContainer {

	public Py4jProtobufClientContainer(ID containerID) {
		super(containerID);
	}

	protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration) {
		return new ProtoBufDirectRemoteService(this, registration);
	}

	public class ProtoBufDirectRemoteService extends AbstractRSAClientService {

		public static final String PROTOBUF_PARSER_STATIC_METHODNAME = "parser";

		private ProtobufCallableEndpoint endpoint;
		private Long rsId;

		public ProtoBufDirectRemoteService(AbstractClientContainer container,
				RemoteServiceClientRegistration registration) {
			super(container, registration);
			this.rsId = registration.getID().getContainerRelativeID();
			Activator a = Activator.getDefault();
			if (a == null)
				throw new NullPointerException("Cannot create ProtoBufDirectRemoteService because bundle not active");
			this.endpoint = a.getCallableEndpoint();
			if (this.endpoint == null)
				throw new NullPointerException(
						"Cannot create ProtoBufDirectRemoteService because no ProtobufCallableEndpoint available");
		}

		@Override
		public void dispose() {
			super.dispose();
			this.endpoint = null;
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

		@Override
		protected Object invokeSync(RSARemoteCall remoteCall) throws ECFException {
			// Checks on arguments/parameters
			Object[] args = remoteCall.getParameters();
			Object message = null;
			if (args != null && args.length > 0) {
				message = args[0];
				if (message != null && !(message instanceof Message))
					throw new ECFException(
							"Remote call=" + remoteCall + " the first parameter must be of type Message");
			}
			Method reflectMethod = remoteCall.getReflectMethod();
			Class<?> returnType = reflectMethod.getReturnType();
			if (returnType == null || returnType.equals(Void.TYPE)) {
				returnType = Void.TYPE;
			} else if (!Message.class.isAssignableFrom(returnType))
				throw new ECFException("Remote call=" + remoteCall + " return type is not Message");

			@SuppressWarnings("rawtypes")
			Parser parser = null;
			if (!returnType.equals(Void.TYPE)) {
				try {
					Method m = returnType.getMethod(PROTOBUF_PARSER_STATIC_METHODNAME, (Class[]) null);
					parser = Parser.class.cast(m.invoke(null, (Object[]) null));
				} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e1) {
					throw new ECFException("Exception getting parser from return type", e1);
				}
			}
			try {
				// Actually make call via ProtobufCallableEndpointImpl
				return endpoint.call_endpoint(rsId, remoteCall.getMethod(), (Message) message, parser);
			} catch (Exception e) {
				throw new ECFException("Could not execute remote call=" + remoteCall, e);
			}
		}

	}
}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.protobuf;

import java.util.concurrent.Callable;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.remoteservice.IRemoteService;
import org.eclipse.ecf.remoteservice.client.AbstractClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientContainer;
import org.eclipse.ecf.remoteservice.client.AbstractRSAClientService;
import org.eclipse.ecf.remoteservice.client.RemoteServiceClientRegistration;
import org.eclipse.ecf.remoteservice.events.IRemoteCallCompleteEvent;

import com.google.protobuf.Message;

public class ProtobufClientContainer extends AbstractRSAClientContainer {

	private final ProtobufCallableEndpoint endpoint;

	public ProtobufClientContainer(ID containerID, ProtobufCallableEndpoint endpoint) {
		super(containerID);
		this.endpoint = endpoint;
	}

	protected IRemoteService createRemoteService(RemoteServiceClientRegistration registration) {
		return new ProtoBufDirectRemoteService(this, registration);
	}

	public class ProtoBufDirectRemoteService extends AbstractRSAClientService {

		public static final String PROTOBUF_PARSER_STATIC_METHODNAME = "parser";

		private Long rsId;

		public ProtoBufDirectRemoteService(AbstractClientContainer container,
				RemoteServiceClientRegistration registration) {
			super(container, registration);
			this.rsId = registration.getID().getContainerRelativeID();
		}

		@Override
		protected Callable<IRemoteCallCompleteEvent> getAsyncCallable(RSARemoteCall call) {
			return () -> {
				Object result = invokeCall(call);
				return createRCCESuccess(result);
			};
		}

		@Override
		protected Callable<Object> getSyncCallable(final RSARemoteCall call) {
			return () -> {
				// Synchronously invoke the call method on the proxy
				Object result = invokeCall(call);
				return result;
			};
		}

		private Object invokeCall(RSARemoteCall remoteCall) throws Exception {
			// Checks on arguments/parameters
			// Actually make call via AbstractProtobufCallableEndpoint
			return endpoint.call_endpoint(rsId, remoteCall.getMethod(), (Message) remoteCall.getParameters()[0]);
		}
	}
}

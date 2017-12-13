/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.protobuf;

import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class ProtobufCallableEndpointImpl implements ProtobufCallableEndpoint {

	private Logger timingLogger = LoggerFactory.getLogger("timing.org.eclipse.ecf.provider.direct.protobuf");

	private ExternalCallableEndpoint eceService;

	public void bindExternalCallableEndpoint(ExternalCallableEndpoint cbv) {
		this.eceService = cbv;
	}

	public void unbindExternalCallableEndpoint() {
		this.eceService = null;
	}

	public ExternalCallableEndpoint getExternalCallableEndpoint() {
		return eceService;
	}

	protected byte[] serializeMessage(Message message) {
		long startTime = 0;
		byte[] messageBytes = null;
		if (message != null) {
			if (timingLogger != null && timingLogger.isDebugEnabled())
				startTime = System.currentTimeMillis();
			messageBytes = message.toByteArray();
			if (timingLogger != null && timingLogger.isDebugEnabled()) {
				long endTime = System.currentTimeMillis();
				timingLogger.debug("protobuf.request.serialize;class=" + message.getClass().getName() + ";dif="
						+ (endTime - startTime)+"ms");
			}
		}
		return messageBytes;
	}

	protected Message deserializeMessage(byte[] resultBytes, Parser<?> resultParser)
			throws InvalidProtocolBufferException {
		long startTime = 0;
		// If result is null/None then return null
		if (resultBytes == null || resultParser == null) 
			return null;
		// Now deserialize result
		if (timingLogger != null && timingLogger.isDebugEnabled())
			startTime = System.currentTimeMillis();
		// Else parse and return Message
		Message result = (Message) resultParser.parseFrom(resultBytes);
		if (timingLogger != null && timingLogger.isDebugEnabled()) {
			long endTime = System.currentTimeMillis();
			timingLogger.debug(
					"protobuf.response.parse;class=" + result.getClass().getName() + ";diff=" + (endTime - startTime)+"ms");
		}
		return result;
	}

	protected byte[] call_endpoint(Long rsId, String methodName, byte[] messageBytes) throws Exception {
		long start = 0;
		if (timingLogger != null && timingLogger.isDebugEnabled())
			start = System.currentTimeMillis();
		byte[] resultBytes = getExternalCallableEndpoint()._call_endpoint(rsId, methodName, messageBytes);
		if (timingLogger != null && timingLogger.isDebugEnabled()) {
			long end = System.currentTimeMillis();
			timingLogger.debug("protobuf.pythonrpc;rsId=" + rsId + ";method=" + methodName + ";time=" + (end - start)+"ms");
		}
		return resultBytes;
	}

	@Override
	public <A extends Message> Message call_endpoint(Long rsId, String methodName, A message, Parser<?> resultParser)
			throws Exception {
		// we only send the method name, not the fully qualified name (with class and
		// method name)
		int dotLastIndex = methodName.lastIndexOf('.');
		if (dotLastIndex >= 0)
			methodName = methodName.substring(dotLastIndex + 1);
		return deserializeMessage(call_endpoint(rsId, methodName, serializeMessage(message)), resultParser);
	}

}

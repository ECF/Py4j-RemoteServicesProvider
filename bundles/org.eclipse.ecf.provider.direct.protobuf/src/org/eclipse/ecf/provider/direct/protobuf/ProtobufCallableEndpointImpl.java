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

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public class ProtobufCallableEndpointImpl implements ProtobufCallableEndpoint {

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

	@Override
	public <A extends Message> Message call_endpoint(Long rsId, String methodName, A message, Parser<?> resultParser)
			throws Exception {
		// we only send the method name, not the fully qualified name (with class and
		// method name)
		int dotLastIndex = methodName.lastIndexOf('.');
		if (dotLastIndex >= 0)
			methodName = methodName.substring(dotLastIndex + 1);
		// Serialized message, and call cbvService
		byte[] resultBytes = getExternalCallableEndpoint()._call_endpoint(rsId, methodName,
				(message == null) ? null : message.toByteArray());
		// If result is null/None then return null
		if (resultBytes == null || resultParser == null)
			return null;
		// Else parse and return Message
		return (Message) resultParser.parseFrom(resultBytes);
	}

}

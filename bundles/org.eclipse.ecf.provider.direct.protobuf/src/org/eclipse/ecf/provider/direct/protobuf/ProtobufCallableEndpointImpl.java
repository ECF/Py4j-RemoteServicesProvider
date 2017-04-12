/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.protobuf;

import org.eclipse.ecf.provider.direct.CallableEndpoint;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

@Component(immediate = true)
public class ProtobufCallableEndpointImpl implements ProtobufCallableEndpoint {

	private CallableEndpoint cbvService;
	private static ProtobufCallableEndpointImpl instance;
	
	public ProtobufCallableEndpointImpl() {
		instance = this;
	}
	
	@Reference
	void bindCallByValueService(CallableEndpoint cbv) {
		this.cbvService = cbv;
	}
	
	void unbindCallByValueService(CallableEndpoint cbv) {
		this.cbvService = null;
	}
	
	@Override
	public <A extends Message> Message call_endpoint(Long rsId, String methodName, A message,
			Parser<?> resultParser) throws Exception {
		byte[] resultBytes = this.cbvService._call_endpoint(rsId, methodName, message.toByteArray());
		if (resultBytes == null)
			return null;
		return (Message) resultParser.parseFrom(resultBytes);
	}
	
	public static ProtobufCallableEndpoint getDefault() {
		return instance;
	}
	
	public CallableEndpoint getCallableEndpoint() {
		return cbvService;
	}
}

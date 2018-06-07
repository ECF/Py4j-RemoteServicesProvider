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
import java.nio.ByteBuffer;

import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

public class FlatbufCallableEndpointImpl implements FlatbufCallableEndpoint {

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

	protected byte[] serializeBuilder(FlatBufferBuilder builder) {
		long startTime = 0;
		byte[] messageBytes = null;
		if (builder != null) {
			if (timingLogger != null && timingLogger.isDebugEnabled())
				startTime = System.currentTimeMillis();
			messageBytes = builder.sizedByteArray();
			if (timingLogger != null && timingLogger.isDebugEnabled()) {
				long endTime = System.currentTimeMillis();
				timingLogger.debug("protobuf.request.serialize;builder=" + builder + ";d="
						+ (endTime - startTime));
			}
		}
		return messageBytes;
	}

	protected Table deserializeTable(byte[] resultBytes, Class<?> clazz) throws Exception {
		long startTime = 0;
		// Now deserialize result
		if (timingLogger != null && timingLogger.isDebugEnabled())
			startTime = System.currentTimeMillis();

		// If result is null/None then return null
		if (resultBytes == null || clazz == null) {
			if (timingLogger != null && timingLogger.isDebugEnabled())
				timingLogger.debug("flatbuf.response.deserialize;null response");
			return null;
		}
		String simpleName = clazz.getSimpleName();
		Method m = clazz.getMethod("getRootAs" + simpleName, new Class<?>[] { ByteBuffer.class });
		if (m == null)
			throw new NullPointerException("Cannot get static method:  getRootAs" + simpleName);
		Table result = (Table) m.invoke(null, ByteBuffer.wrap(resultBytes));
		if (timingLogger != null && timingLogger.isDebugEnabled()) {
			long endTime = System.currentTimeMillis();
			timingLogger.debug(
					"protobuf.response.parse;class=" + result.getClass().getName() + ";d=" + (endTime - startTime));
		}
		return result;
	}

	protected byte[] call_endpoint(Long rsId, String methodName, byte[] messageBytes) throws Exception {
		long start = 0;
		if (timingLogger != null && timingLogger.isDebugEnabled())
			start = System.currentTimeMillis();
		byte[] resultBytes = (byte[]) getExternalCallableEndpoint()._call_endpoint(rsId, methodName, messageBytes);
		if (timingLogger != null && timingLogger.isDebugEnabled()) {
			long end = System.currentTimeMillis();
			timingLogger.debug("protobuf.pythonrpc;rsId=" + rsId + ";method=" + methodName + ";time=" + (end - start));
		}
		return resultBytes;
	}

	@Override
	public Table call_endpoint(Long rsId, String methodName, FlatBufferBuilder builder, Class<?> returnType) throws Exception {
		// we only send the method name, not the fully qualified name (with class and
		// method name)
		int dotLastIndex = methodName.lastIndexOf('.');
		if (dotLastIndex >= 0)
			methodName = methodName.substring(dotLastIndex + 1);
		return deserializeTable(call_endpoint(rsId, methodName, serializeBuilder(builder)), returnType);
	}

}

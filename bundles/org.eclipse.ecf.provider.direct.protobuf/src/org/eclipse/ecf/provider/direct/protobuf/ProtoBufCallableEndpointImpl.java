package org.eclipse.ecf.provider.direct.protobuf;

import org.eclipse.ecf.provider.direct.CallableEndpoint;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

@Component(immediate = true)
public class ProtoBufCallableEndpointImpl implements ProtoBufCallableEndpoint {

	private CallableEndpoint cbvService;
	
	@Reference
	void bindCallByValueService(CallableEndpoint cbv) {
		this.cbvService = cbv;
	}
	
	void unbindCallByValueService(CallableEndpoint cbv) {
		this.cbvService = null;
	}
	
	protected <R extends Message> R deserializeMessage(byte[] result, Parser<R> resultParser) throws Exception {
		return resultParser.parseFrom(result);
	}

	protected byte[] serializeMessage(Message message) {
		return message.toByteArray();
	}

	@Override
	public <A extends Message, R extends Message> R call_endpoint(String endpointid, String methodName, A message,
			Parser<R> resultParser) throws Exception {
		byte[] resultBytes = this.cbvService._call_endpoint(endpointid, methodName, serializeMessage(message));
		return deserializeMessage(resultBytes, resultParser);
	}
}

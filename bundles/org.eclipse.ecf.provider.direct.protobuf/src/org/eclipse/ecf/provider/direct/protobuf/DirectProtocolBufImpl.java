package org.eclipse.ecf.provider.direct.protobuf;

import org.eclipse.ecf.provider.direct.CallByValueService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

@Component(immediate = true)
public class DirectProtocolBufImpl implements DirectProtocolBuf {

	private CallByValueService cbvService;
	
	@Reference
	void bindCallByValueService(CallByValueService cbv) {
		this.cbvService = cbv;
	}
	
	void unbindCallByValueService(CallByValueService cbv) {
		this.cbvService = null;
	}
	
	public Message callDirect(String id, String methodName, Message message, Parser<? extends Message> resultParser) throws Exception {
		byte[] resultBytes = this.cbvService.callByValue(id, methodName, serializeMessage(message));
		return deserializeResult(resultBytes, resultParser);
	}

	protected Message deserializeResult(byte[] result, Parser<? extends Message> resultParser) throws Exception {
		return resultParser.parseFrom(result);
	}

	protected byte[] serializeMessage(Message message) {
		return message.toByteArray();
	}
}

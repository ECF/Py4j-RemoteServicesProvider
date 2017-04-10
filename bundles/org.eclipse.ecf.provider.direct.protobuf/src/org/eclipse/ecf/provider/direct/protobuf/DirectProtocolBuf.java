package org.eclipse.ecf.provider.direct.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public interface DirectProtocolBuf {

	Message callDirect(String id, String methodName, Message message, Parser<? extends Message> resultParser) throws Exception;

}
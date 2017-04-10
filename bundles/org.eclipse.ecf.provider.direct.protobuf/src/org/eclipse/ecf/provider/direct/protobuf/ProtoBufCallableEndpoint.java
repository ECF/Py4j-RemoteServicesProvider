package org.eclipse.ecf.provider.direct.protobuf;

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

public interface ProtoBufCallableEndpoint {

	<A extends Message, R extends Message> R call_endpoint(String endpointid, String methodName, A message, Parser<R> resultParser) throws Exception;

}
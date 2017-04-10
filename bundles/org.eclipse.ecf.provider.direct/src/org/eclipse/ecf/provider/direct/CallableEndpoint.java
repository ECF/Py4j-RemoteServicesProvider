package org.eclipse.ecf.provider.direct;

public interface CallableEndpoint {

	byte[] _call_endpoint(String endpointid, String methodName, byte[] serializedArgs) throws Exception;
	
}

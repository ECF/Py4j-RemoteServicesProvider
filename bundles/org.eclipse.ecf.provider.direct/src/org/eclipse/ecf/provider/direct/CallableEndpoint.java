package org.eclipse.ecf.provider.direct;

public interface CallableEndpoint {

	byte[] _call_endpoint(Long rsId, String methodName, byte[] serializedArgs) throws Exception;
	
}

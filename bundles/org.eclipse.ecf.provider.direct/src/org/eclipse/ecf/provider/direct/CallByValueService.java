package org.eclipse.ecf.provider.direct;

public interface CallByValueService {

	byte[] callByValue(String id, String methodName, byte[] serializedArgs) throws Exception;
	
}

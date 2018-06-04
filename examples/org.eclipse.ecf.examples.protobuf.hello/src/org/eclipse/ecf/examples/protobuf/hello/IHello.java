package org.eclipse.ecf.examples.protobuf.hello;

import java.util.concurrent.CompletionStage;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;

public interface IHello {

	HelloMsgContent sayHello(HelloMsgContent message) throws Exception;
	
	CompletionStage<HelloMsgContent> sayHelloAsync(HelloMsgContent message) throws Exception;
	
}

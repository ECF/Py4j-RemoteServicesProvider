package org.eclipse.ecf.examples.protobuf.hello;

import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;

public interface IHelloAsync {

	CompletableFuture<HelloMsgContent> sayHelloAsync(HelloMsgContent message) throws Exception;
}

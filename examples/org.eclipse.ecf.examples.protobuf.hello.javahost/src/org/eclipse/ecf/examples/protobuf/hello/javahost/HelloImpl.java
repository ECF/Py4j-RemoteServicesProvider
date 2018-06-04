package org.eclipse.ecf.examples.protobuf.hello.javahost;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.host.pb",
		"service.intents=osgi.async" })
public class HelloImpl implements IHello {

	HelloMsgContent createResponse(String method) {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setF(method);
		b1.setTo("python");
		b1.setHellomsg("Hello response from " + method);
		b1.setH("howdy");
		return b1.build();
	}

	@Override
	public HelloMsgContent sayHello(HelloMsgContent message) throws Exception {
		System.out.println("sayHello called with message=" + message);
		return createResponse("sayHello");
	}

	@Override
	public CompletionStage<HelloMsgContent> sayHelloAsync(HelloMsgContent message) throws Exception {
		System.out.println("sayHelloAsync called with message=" + message);
		CompletableFuture<HelloMsgContent> result = new CompletableFuture<HelloMsgContent>();
		result.complete(createResponse("sayHelloAsync"));
		return result;
	}

}

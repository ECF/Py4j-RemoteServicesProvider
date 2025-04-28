package org.eclipse.ecf.examples.protobuf.hello.javahost;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

@Component(immediate = true, property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.protobuf.host",
		"service.intents=osgi.async", "osgi.basic.timeout:Long=45000" })
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
		System.out.println("pb.sayHello called with message=" + message);
		return createResponse("sayHello");
	}

	@Override
	public CompletionStage<HelloMsgContent> sayHelloAsync(HelloMsgContent message) throws Exception {
		System.out.println("pb.sayHelloAsync called with message=" + message);
		CompletableFuture<HelloMsgContent> result = new CompletableFuture<HelloMsgContent>();
		result.complete(createResponse("sayHelloAsync"));
		return result;
	}

	@Override
	public Promise<HelloMsgContent> sayHelloPromise(HelloMsgContent message) throws Exception {
		System.out.println("pb.sayHelloPromise called with message=" + message);
		Deferred<HelloMsgContent> result = new Deferred<HelloMsgContent>();
		result.resolve(createResponse("sayHelloPromise"));
		return result.getPromise();
	}

}

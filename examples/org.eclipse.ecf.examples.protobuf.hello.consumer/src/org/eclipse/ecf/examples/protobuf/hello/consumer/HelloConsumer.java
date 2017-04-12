package org.eclipse.ecf.examples.protobuf.hello.consumer;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsg;
import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class HelloConsumer {

	private IHello helloService;
	
	@Reference
	void bindHello(IHello hello) {
		this.helloService = hello;
	}
	
	void unbindHello(IHello hello) {
		this.helloService = null;
	}
	
	HelloMsg createRequest() {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setFrom("me");
		b1.setTo("you");
		b1.setHellomsg("Hello 1");
		HelloMsgContent.Builder b2 = HelloMsgContent.newBuilder();
		b2.addX(2.1);
		b2.addX(2.2);
		b2.setFrom("stacy");
		b2.setTo("bob");
		b2.setHellomsg("Hello 2");
		HelloMsg.Builder resultBuilder = HelloMsg.newBuilder();
		resultBuilder.addMsgs(0, b1.build());
		resultBuilder.addMsgs(1, b2.build());
		return resultBuilder.build();
	}
	void activate() {
		try {
			HelloMsg request = createRequest();
			HelloMsg result = this.helloService.sayHello(request);
			System.out.println("Received result="+result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

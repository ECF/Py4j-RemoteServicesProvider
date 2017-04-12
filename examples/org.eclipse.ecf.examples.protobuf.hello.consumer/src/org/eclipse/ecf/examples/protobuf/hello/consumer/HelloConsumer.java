package org.eclipse.ecf.examples.protobuf.hello.consumer;

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
	
	HelloMsgContent createRequest() {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setF("me");
		b1.setTo("you");
		b1.setHellomsg("Hello 1");
		b1.setH("hval");
		return b1.build();
	}
	void activate() {
		try {
			HelloMsgContent request = createRequest();
			HelloMsgContent result = this.helloService.sayHello(request);
			System.out.println("Received result="+result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

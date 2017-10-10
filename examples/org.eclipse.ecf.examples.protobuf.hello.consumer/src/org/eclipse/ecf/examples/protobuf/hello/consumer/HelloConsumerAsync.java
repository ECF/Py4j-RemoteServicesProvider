package org.eclipse.ecf.examples.protobuf.hello.consumer;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.eclipse.ecf.examples.protobuf.hello.IHelloAsync;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class HelloConsumerAsync {

	private IHelloAsync helloService;
	
	@Reference
	void bindHello(IHelloAsync hello) {
		this.helloService = hello;
		System.out.println("IHelloAsync service bound="+hello);
	}
	
	void unbindHello(IHello hello) {
		this.helloService = null;
		System.out.println("IHelloAsync service unbound"+hello);
	}
	
	HelloMsgContent createRequest() {
		HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
		b1.addX(1.1);
		b1.addX(1.2);
		b1.setF("java");
		b1.setTo("python");
		b1.setHellomsg("Hello from java");
		b1.setH("some other message");
		return b1.build();
	}
	@Activate
	void activate() {
		try {
			HelloMsgContent.Builder b1 = HelloMsgContent.newBuilder();
			b1.addX(1.1);
			b1.addX(1.2);
			b1.setF("fromjava");
			b1.setTo("topython");
			b1.setHellomsg("Hello message from java!");
			b1.setH("An additional message from java");
			HelloMsgContent request = b1.build();
			this.helloService.sayHelloAsync(request).whenComplete((r,e) -> {
				if (e != null)
					e.printStackTrace();
				else
					System.out.println("Received async result="+r);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

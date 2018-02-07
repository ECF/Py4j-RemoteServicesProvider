package org.eclipse.ecf.examples.protobuf.hello.consumer;

import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.eclipse.ecf.examples.protobuf.hello.IHelloAsync;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import py4j.PythonThrowable;

//@Component(immediate = true)
public class HelloConsumerAsync {

	private IHelloAsync helloService;
	
	@Reference
	void bindHello(IHelloAsync hello) {
		this.helloService = hello;
	}
	
	void unbindHello(IHello hello) {
		this.helloService = null;
	}
	
	@Activate
	void activate() {
		try {
			this.helloService.sayHelloAsync(HelloConsumer.createRequest()).whenComplete((r,e) -> {
				if (e != null) {
					PythonThrowable pt = (PythonThrowable) e.getCause().getCause().getCause();
					pt.printStackTrace();
					
					e.printStackTrace();
					e.printStackTrace();
				} else
					System.out.println("Received sayHelloAsync result="+r);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

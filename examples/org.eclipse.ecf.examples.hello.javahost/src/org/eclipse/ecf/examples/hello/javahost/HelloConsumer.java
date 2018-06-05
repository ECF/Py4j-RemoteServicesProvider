package org.eclipse.ecf.examples.hello.javahost;

import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.examples.hello.IHello;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class HelloConsumer {

	private IHello helloservice;
	
	@Reference(target="(service.imported=*)")
	void bindHello(IHello hello) {
		this.helloservice = hello;
		
		String result = this.helloservice.sayHello("Java","Hello Python");
		System.out.println("Java received result="+result);
		
		CompletableFuture<String> cf = this.helloservice.sayHelloAsync("JavaAsync","Howdy Python");
		cf.whenComplete((r,t) -> {
			if (t != null)
				t.printStackTrace();
			else
				System.out.println("JavaAsync received result="+r);
		});
		
	}
	
	void unbindHello(IHello hello) {
		this.helloservice = null;
	}
}

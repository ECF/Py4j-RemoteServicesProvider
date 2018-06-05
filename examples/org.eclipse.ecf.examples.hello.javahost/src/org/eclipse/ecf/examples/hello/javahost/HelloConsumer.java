package org.eclipse.ecf.examples.hello.javahost;

import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.examples.hello.IHello;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

@Component
/**
 * Example consumer of IHello remote service.  The implementation of this remote service is 
 * implemented in Python in this example, but that is not relevant to the consumer.
 *
 */
public class HelloConsumer {

	private IHello helloservice;
	
	@Reference(target="(service.imported=*)")
	/**
	 * method called by DS to bind a IHello remote service when imported.  Injects the IHello instance
	 * and calls the service methods, both synchronous (sayHello) and asynchronous (sayHelloAsync and
	 * sayHelloPromise.  See the IHello service interface for the service contract.
	 * The (service.imported=*) requires that the service be a remote service rather than
	 * a local service instance.  This property is defined by the <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html">OSGi Remote Service
	 * specification</a>.
	 * @param hello the IHello instance injected by declarative services when the remote service is
	 * imported.
	 */
	void bindHello(IHello hello) {
		this.helloservice = hello;
		
		// Call remote (Python) sayHello method.  This will block until a result is provided,
		// or until the value of osgi.basic.timeout value has expired (set by Python implementation)
		String result = this.helloservice.sayHello("Java","Hello Python");
		System.out.println("Java received result="+result);
		
		// Call remote (Python) sayHelloAsync method
		CompletableFuture<String> cf = this.helloservice.sayHelloAsync("JavaAsync","Howdy Python");
		cf.whenComplete((resp,except) -> {
			if (except != null)
				except.printStackTrace();
			else
				System.out.println("sayHelloAsync received result="+resp);
		});
		System.out.println("done with calling sayHelloAsync");
		// Call remote (Python sayHelloPromise implementation
		Promise<String> promise = this.helloservice.sayHelloPromise("JavaPromise","Howdy Python");
		promise.onResolve(new Runnable() {
			public void run() {
				try {
					System.out.println("sayHelloPromise received result="+promise.getValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
		}});
		System.out.println("done with calling sayHelloPromise");
	}
	
	void unbindHello(IHello hello) {
		this.helloservice = null;
	}
}

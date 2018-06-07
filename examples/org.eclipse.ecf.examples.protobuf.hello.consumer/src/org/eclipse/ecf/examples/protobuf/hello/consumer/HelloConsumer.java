package org.eclipse.ecf.examples.protobuf.hello.consumer;

import java.util.concurrent.CompletionStage;

import org.eclipse.ecf.examples.protobuf.hello.IHello;
import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

@Component(immediate = true)
/**
 * Example consumer of IHello remote service (implemented in Python).
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
	}
	
	void unbindHello(IHello hello) {
		this.helloservice = null;
	}
	
	static HelloMsgContent createRequestMessage() {
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
	void activate() throws Exception {
		// Call remote sayHello method.  This will block until a result is provided,
		// or until the value of osgi.basic.timeout value has expired (set by Python implementation)
		HelloMsgContent result = this.helloservice.sayHello(createRequestMessage());
		System.out.println("pb.sayHello received result="+result);
		
		// Call remote sayHelloAsync method
		CompletionStage<HelloMsgContent> cf = this.helloservice.sayHelloAsync(createRequestMessage());
		cf.whenComplete((resp,except) -> {
			if (except != null)
				except.printStackTrace();
			else
				System.out.println("pb.sayHelloAsync received result="+resp);
		});
		System.out.println("done with calling pb.sayHelloAsync");
		// Call remote sayHelloPromise implementation
		Promise<HelloMsgContent> promise = this.helloservice.sayHelloPromise(createRequestMessage());
		promise.onResolve(new Runnable() {
			public void run() {
				try {
					System.out.println("pbsayHelloPromise received result="+promise.getValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
		}});
		System.out.println("done with calling pbsayHelloPromise");
	}

}

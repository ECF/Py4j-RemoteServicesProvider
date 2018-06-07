package org.eclipse.ecf.examples.protobuf.hello;

import java.util.concurrent.CompletionStage;

import org.eclipse.ecf.examples.protobuf.hello.Hellomsg.HelloMsgContent;
import org.osgi.util.promise.Promise;

/**
 * Example remote service interface using protocol buffers messages as argument and 
 * return type.  Has async methods (CompletableFuture<String> and Promise<String>
 * return types), as defined by the 
 * <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html#d0e1407">Asynchronous Remote Services</a>
 * specification.   As per the intent, when invoked remotely, these methods will immediately
 * return a CompletableFuture or Promise that will subsequently return a result of declared type (String in below).
 * If the osgi.basic.timeout service property is set, the CompletableFuture or promise will timeout
 * after the given milliseconds.
 *
 */
public interface IHello {

	/**
	 * Synchronously provide protobuf HelloMsgContent and receive a
	 * HelloMsgContent response.
	 *  
	 * @param message the HelloMsgContent instance to send
	 * @return HelloMsgResponse from remote service after synchronously invoking remote method
	 */
	HelloMsgContent sayHello(HelloMsgContent message) throws Exception;
	
	/**
	 * Asynchronously invoke remote method and provide HelloMsgContent result via returned
	 * CompletableFuture.
	 * 
	 * @param message the HelloMsgContent instance to send
	 * @return CompletableFuture that invokes remote method asynchronously to complete.  
	 * Will throw java.util.concurrent.TimeoutException if response is not
	 * received in within timeout provided via osgi.basic.timeout service property on impl
	 */
	CompletionStage<HelloMsgContent> sayHelloAsync(HelloMsgContent message) throws Exception;
	
	/**
	 * Asynchronously invoke remote method and provide HelloMsgContent result via returned 
	 * Promise.
	 * 
	 * @param message the HelloMsgContent instance to send
	 * @return Promise that invokes remote method asynchronously to complete.  
	 * Will throw java.util.concurrent.TimeoutException if response is not
	 * received in within timeout provided via osgi.basic.timeout service property on impl
	 */
	Promise<HelloMsgContent> sayHelloPromise(HelloMsgContent message) throws Exception;

}

/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.hello;

import java.util.concurrent.CompletableFuture;

import org.osgi.util.promise.Promise;

/**
 * Example remote service interface.  Has async methods (CompletableFuture<String> and Promise<String>
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
	 * Synchronously provide 'from' and 'message' Strings to service, and receive a
	 * String response.
	 *  
	 * @param from a String identifying the sender
	 * @param message a String message
	 * @return String response from remote service after synchronously invoking remote method
	 */
	String sayHello(String from, String message);
	
	/**
	 * Asynchronously invoke remote method and provide String result via returned
	 * CompletableFuture.
	 * 
	 * @param from a String identifying the sender
	 * @param message a String message
	 * @return CompletableFuture that invokes remote method asynchronously to complete.  
	 * Will throw java.util.concurrent.TimeoutException if response is not
	 * received in within timeout provided via osgi.basic.timeout intent
	 */
	CompletableFuture<String> sayHelloAsync(String from, String message);
	
	/**
	 * Asynchronously invoke remote method and provide String result via returned 
	 * Promise.
	 * 
	 * @param from a String identifying the sender
	 * @param message a String message
	 * @return Promis that invokes remote method asynchronously to complete.  
	 * Will throw java.util.concurrent.TimeoutException if response is not
	 * received in within timeout provided via osgi.basic.timeout intent
	 */
	Promise<String> sayHelloPromise(String from, String message);
	
}

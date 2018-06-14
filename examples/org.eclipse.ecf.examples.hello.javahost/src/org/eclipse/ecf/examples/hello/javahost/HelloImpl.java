/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.hello.javahost;

import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.examples.hello.IHello;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

@Component(immediate=true,enabled=false,property = { "service.exported.interfaces=*",  // RS/RSA-required to export as remote service
									   "service.exported.configs=ecf.py4j.host",  // Required to specify py4j java provider
									   "osgi.basic.timeout:Long=50000",    // timeout of 50000ms=50 seconds
									   "service.intents=osgi.async"}) // osgi.async intent to get the behavior
																	   // defined <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.remoteservices.html#d0e1407">here</a>.
/**
 * Example impl of IHello remote service with asynchronous remote methods
 */
public class HelloImpl implements IHello {

	@Override
	public String sayHello(String from, String message) {
		System.out.println("Java.sayHello called by "+from+" with message: '"+message+"'");
		return "Java says: Hi "+from + ", nice to see you";
	}

	@Override
	public CompletableFuture<String> sayHelloAsync(String from, String message) {
		System.out.println("Java.sayHelloAsync called by "+from+" with message: '"+message+"'");
		CompletableFuture<String> result = new CompletableFuture<String>();
		// Simplest impl is to complete right away
		result.complete("JavaAsync says: Hi "+from + ", nice to see you");
		// Must return a non-null CompletableFuture instance
		return result;
	}

	@Override
	public Promise<String> sayHelloPromise(String from, String message) {
		System.out.println("Java.sayHelloPromise called by "+from+" with message: '"+message+"'");
		Deferred<String> deferred = new Deferred<String>();
		// Simplest impl is to resolve right away, but this is not required
		deferred.resolve("JavaPromise says: Hi "+from + ", nice to see you");
		// Must return a non-null Promise instance
		return deferred.getPromise();
	}

}

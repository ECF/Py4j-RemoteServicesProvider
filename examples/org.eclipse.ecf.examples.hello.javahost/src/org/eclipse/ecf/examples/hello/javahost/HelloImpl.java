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

@Component(immediate=true,property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.host","osgi.basic.timeout=50","service.intents=py4j.async"})
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
		result.complete("JavaAsync says: Hi "+from + ", nice to see you");
		return result;
	}

}

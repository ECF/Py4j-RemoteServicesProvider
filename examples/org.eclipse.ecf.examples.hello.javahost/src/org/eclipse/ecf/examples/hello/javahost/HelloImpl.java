/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.hello.javahost;

import org.eclipse.ecf.examples.hello.IHello;
import org.eclipse.ecf.provider.py4j.Py4jProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate=true,property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.host"})
public class HelloImpl implements IHello {

	@Reference
	// Making this a reference waits to activate and export this component until 
	// there is a Py4jProvider available
	void bindPy4jDirectProvider(Py4jProvider provider) {
	}
	
	void unbindPy4jDirectProvider(Py4jProvider provider) {
	}
	
	@Override
	public String sayHello(String from, String message) {
		System.out.println("sayHello from: "+from+" with message:"+message);
		return "Hi "+from + ", nice to see you";
	}

}

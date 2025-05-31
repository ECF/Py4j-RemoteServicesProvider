/*******************************************************************************
 * Copyright (c) 2025 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.ai.mcp.toolservice.impl;

import org.eclipse.ecf.examples.ai.mcp.toolservice.api.ArithmeticTools;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, property = { "service.exported.interfaces=*", "service.exported.configs=ecf.py4j.host" })
public class ArithmeticToolsImpl implements ArithmeticTools {

	public int add(int a, int b) {
		return a + b;
	}

	@Override
	public int multiply(int a, int b) {
		return a * b;
	}

}

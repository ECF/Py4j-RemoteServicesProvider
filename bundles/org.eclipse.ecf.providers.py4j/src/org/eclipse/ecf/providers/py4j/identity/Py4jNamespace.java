/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.providers.py4j.identity;

import org.eclipse.ecf.core.identity.URIID.URIIDNamespace;
import org.eclipse.ecf.providers.py4j.Py4jConstants;

public class Py4jNamespace extends URIIDNamespace {

	private static final long serialVersionUID = 7911328070175061027L;
	public static Py4jNamespace INSTANCE;

	public Py4jNamespace() {
		super(Py4jConstants.NAMESPACE_NAME, "Py4j Namespace");
		INSTANCE = this;
	}

	public static Py4jNamespace getInstance() {
		return INSTANCE;
	}

	public String getScheme() {
		return "py4j";
	}
}

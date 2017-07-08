/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.identity;

import java.util.UUID;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.core.identity.URIID.URIIDNamespace;
import org.eclipse.ecf.provider.py4j.Py4jConstants;

import py4j.GatewayServer;

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
		return Py4jConstants.NAMESPACE_NAME;
	}

	public static ID createPy4jID(String hostname, int port) {
		if (port < 1)
			throw new NullPointerException("port must be > 0");
		if (hostname == null)
			hostname = GatewayServer.DEFAULT_ADDRESS;
		return INSTANCE.createInstance(new Object[] { Py4jConstants.NAMESPACE_PROTOCOL + "://" + hostname + ":"
				+ String.valueOf(port) + Py4jConstants.NAMESPACE_JAVA_PATH });
	}

	public static ID createPy4jID(int port) {
		return createPy4jID(null, port);
	}

	public static ID createUUID() throws IDCreateException {
		return INSTANCE.createInstance(new Object[] { "uuid:" + UUID.randomUUID().toString() });
	}

}

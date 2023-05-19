/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

/**
 * An external service provider may need to provide a way for the OSGi remote
 * service to get a hold of the proxy, and this interface may be used to do so.
 * For example, a service exposed by a Python class will need to provide a way
 * for the RSA-constructed proxy to call the underlying proxy. This interface
 * allows remote service client containers (e.g. DirectClientContainer) to get
 * access to the underlying proxy so that it can be called when appropriate.
 * 
 * @author slewis
 *
 */
public interface ExternalServiceProvider {

	public static final String PROXYID_PROP_NAME = ExternalServiceProvider.class.getName() + ".proxyid";

	Object getProxy(String proxyId);

}

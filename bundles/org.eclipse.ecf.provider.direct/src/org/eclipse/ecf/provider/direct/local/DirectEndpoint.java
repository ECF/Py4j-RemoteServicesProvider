/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.local;

import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;

class DirectEndpoint {
	private EndpointDescription ed;
	private Object proxy;

	public DirectEndpoint(EndpointDescription ed, Object proxy) {
		this.ed = ed;
		this.proxy = proxy;
	}

	public Object getProxy() {
		return this.proxy;
	}

	public EndpointDescription getEndpointDescription() {
		return this.ed;
	}
}
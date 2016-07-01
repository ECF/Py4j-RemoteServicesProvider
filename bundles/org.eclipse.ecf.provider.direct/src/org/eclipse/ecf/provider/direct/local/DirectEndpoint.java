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
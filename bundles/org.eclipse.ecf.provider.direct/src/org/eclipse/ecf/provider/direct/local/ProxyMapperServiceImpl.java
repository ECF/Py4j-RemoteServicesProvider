/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.local;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;

@Component(immediate = true)
public class ProxyMapperServiceImpl implements ProxyMapperService {

	private Map<Long, DirectEndpoint> proxyMap = new HashMap<Long, DirectEndpoint>();
	private List<EndpointEventListener> eels = new ArrayList<EndpointEventListener>();

	@Reference()
	void bindEndpointEventListener(EndpointEventListener l) {
		synchronized (this.eels) {
			this.eels.add(l);
		}
	}

	void unbindEndpointEventListener(EndpointEventListener l) {
		synchronized (this.eels) {
			this.eels.remove(l);
		}
	}

	void fireEndpointEvent(int type, EndpointDescription ed) {
		List<EndpointEventListener> eels = null;
		synchronized (this.eels) {
			eels = new ArrayList<EndpointEventListener>(this.eels);
		}
		for (EndpointEventListener l : eels)
			l.endpointChanged(new EndpointEvent(type, ed),
					"(" + RemoteConstants.ENDPOINT_CONTAINER_ID_NAMESPACE + "=*)");
	}

	public Object getProxy(long proxyid) {
		DirectEndpoint de = null;
		synchronized (this.proxyMap) {
			de = proxyMap.get(proxyid);
		}
		return (de == null) ? null : de.getProxy();
	}

	public void clear() {
		Collection<DirectEndpoint> des = null;
		synchronized (this.proxyMap) {
			System.out.println("clear()");
			des = proxyMap.values();
			proxyMap.clear();
		}
		if (des != null)
			for (DirectEndpoint de : des)
				fireEndpointEvent(EndpointEvent.REMOVED, de.getEndpointDescription());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription createEndpointDescription(
			Map rsaProps) {
		return new org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription(
				PropertiesUtil.conditionProperties(rsaProps));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void exportService(Object proxy, Map rsaProps) {
		if (proxy == null)
			throw new NullPointerException("Proxy cannot be null");
		System.out.println("discoverProxy props=" + rsaProps);
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		long rsId = ed.getRemoteServiceId();
		synchronized (this.proxyMap) {
			proxyMap.put(rsId, new DirectEndpoint(ed, proxy));
		}
		fireEndpointEvent(EndpointEvent.ADDED, ed);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void unexportService(Map rsaProps) {
		System.out.println("undiscoverProxy props" + rsaProps);
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		long rsId = ed.getRemoteServiceId();
		DirectEndpoint de = null;
		synchronized (this.proxyMap) {
			de = proxyMap.remove(rsId);
		}
		if (de != null)
			fireEndpointEvent(EndpointEvent.REMOVED, de.getEndpointDescription());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void updateService(Map rsaProps) {
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		long rsId = ed.getRemoteServiceId();
		boolean found = false;
		synchronized (this.proxyMap) {
			found = this.proxyMap.containsKey(rsId);
		}
		if (found)
			fireEndpointEvent(EndpointEvent.MODIFIED, ed);
	}
}

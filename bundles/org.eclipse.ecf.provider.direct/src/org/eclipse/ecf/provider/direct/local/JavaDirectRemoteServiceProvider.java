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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

public class JavaDirectRemoteServiceProvider implements DirectRemoteServiceProvider, ProxyMapper {

	private RemoteServiceAdmin rsa;
	private final List<ImportRegistration> importRegistrations = Collections
			.synchronizedList(new ArrayList<ImportRegistration>());
	private final Map<Long, Object> proxyMap = Collections.synchronizedMap(new HashMap<Long, Object>());

	public JavaDirectRemoteServiceProvider(RemoteServiceAdmin rsa) {
		this.rsa = rsa;
	}

	public Object getProxy(long proxyid) {
		Object result = proxyMap.get(proxyid);
		System.out.println("getProxy proxyid=" + proxyid);
		return result;
	}

	public void clear() {
		List<ImportRegistration> removedRegs = null;
		synchronized (this) {
			System.out.println("clear()");
			proxyMap.clear();
			removedRegs = removeImportRegistration(null);
		}
		closeImportRegs(removedRegs);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription createEndpointDescription(
			Map rsaProps) {
		if (rsaProps == null)
			throw new NullPointerException("RSA props cannot be null");
		return new org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription(
				PropertiesUtil.conditionProperties(rsaProps));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void registerService(Object proxy, Map rsaProps) {
		if (proxy == null)
			throw new NullPointerException("Proxy cannot be null");
		System.out.println("discoverProxy props=" + rsaProps);
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		long rsId = ed.getRemoteServiceId();
		synchronized (this) {
			System.out.println("proxyMap rsvcids=" + proxyMap.keySet());
			System.out.println("discoverProxy put rsId=" + rsId);
			proxyMap.put(rsId, proxy);
			ImportRegistration reg = rsa.importService(ed);
			Throwable t = reg.getException();
			if (t != null) {
				proxyMap.remove(rsId);
				throw new RuntimeException("Exception on import", t);
			}
			importRegistrations.add(reg);
		}
	}

	protected List<ImportRegistration> removeImportRegistration(
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed) {
		List<ImportRegistration> results = new ArrayList<ImportRegistration>();
		synchronized (this) {
			for (Iterator<ImportRegistration> i = importRegistrations.iterator(); i.hasNext();) {
				ImportRegistration reg = i.next();
				ImportReference ir = reg.getImportReference();
				if (ed == null || (ir != null && ir.getImportedEndpoint().equals(ed))) {
					i.remove();
					results.add(reg);
				}
			}
		}
		return results;
	}

	protected void closeImportRegs(List<ImportRegistration> regs) {
		for (ImportRegistration reg : regs) {
			try {
				System.out.println("closing import reg=" + reg);
				reg.close();
			} catch (Exception e) {
				// XXX should log
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void unregisterService(Map rsaProps) {
		System.out.println("undiscoverProxy props" + rsaProps);
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		long rsId = ed.getRemoteServiceId();
		List<ImportRegistration> removedRegs = null;
		synchronized (this) {
			System.out.println("undiscoverProxy rsvcids=" + proxyMap.keySet() + ",rsId=" + rsId);
			proxyMap.remove(rsId);
			removedRegs = removeImportRegistration(ed);
		}
		closeImportRegs(removedRegs);
	}
}

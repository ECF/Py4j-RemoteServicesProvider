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
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin;
import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

@Component(immediate = true)
public class LocalContainerExporter implements ContainerExporter, RemoteServiceAdminListener {

	private Object lock = new Object();
	private List<DirectRemoteServiceProvider> drsps = new ArrayList<DirectRemoteServiceProvider>();
	private Map<Long, DirectEndpoint> exportedEndpoints = new HashMap<Long, DirectEndpoint>();
	private Map<Long, Object> unexportedEndpoints = new HashMap<Long, Object>();

	@Reference(cardinality=ReferenceCardinality.MULTIPLE,policy=ReferencePolicy.DYNAMIC,target = "(&(objectClass=org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider)(" + DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP + "=*))")
	void bindDirectRemoteServiceProvider(DirectRemoteServiceProvider p) {
		synchronized (lock) {
			for(Long key: exportedEndpoints.keySet()) {
				DirectEndpoint de = exportedEndpoints.get(key);
				if (de != null)
					p.exportService(de.getProxy(), de.getEndpointDescription().getProperties());
			}
			drsps.add(p);
		}
	}

	void unbindDirectRemoteServiceProvider(DirectRemoteServiceProvider p) {
		synchronized (lock) {
			drsps.remove(p);
		}
	}

	void exportDirectEndpoint0(DirectRemoteServiceProvider p, Collection<DirectEndpoint> deps) {
		for (DirectEndpoint de : deps)
			try {
				p.exportService(de.getProxy(), de.getEndpointDescription().getProperties());
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	void exportDirectEndpoint(DirectRemoteServiceProvider p, Collection<DirectEndpoint> deps) {
		if (p != null) {
			exportDirectEndpoint0(p, deps);
		} else {
			for (DirectRemoteServiceProvider drsp : this.drsps)
				exportDirectEndpoint0(drsp, deps);
		}
	}

	@Override
	public void exportFromContainer(long rsvcid, Object proxy) {
		synchronized (lock) {
			this.unexportedEndpoints.put(rsvcid, proxy);
		}
	}

	@Override
	public void remoteAdminEvent(RemoteServiceAdminEvent event) {
		if (event instanceof RemoteServiceAdmin.RemoteServiceAdminEvent) {
			RemoteServiceAdmin.RemoteServiceAdminEvent rsaEvent = (RemoteServiceAdmin.RemoteServiceAdminEvent) event;
			Throwable t = rsaEvent.getException();
			if (t == null) {
				EndpointDescription ed = rsaEvent.getEndpointDescription();
				long rsvcid = ed.getRemoteServiceId();
				Object svc = null;
				synchronized (lock) {
					switch (event.getType()) {
					case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
						svc = unexportedEndpoints.get(rsvcid);
						if (svc != null)
							fireServiceChange(EndpointEvent.ADDED, ed, svc);
						unexportedEndpoints.remove(rsvcid);
						exportedEndpoints.put(rsvcid,new DirectEndpoint(ed, svc));
						break;
					case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
						svc = exportedEndpoints.get(rsvcid);
						if (svc != null)
								fireServiceChange(EndpointEvent.REMOVED, ed, svc);
						exportedEndpoints.remove(rsvcid);
						break;
					case RemoteServiceAdminEvent.EXPORT_UPDATE:
						svc = exportedEndpoints.get(rsvcid);
						if (svc != null)
							fireServiceChange(EndpointEvent.MODIFIED, ed, svc);
						break;
					default:
							break;
					}
				}
			}
		}
	}

	private void fireServiceChange(int type, EndpointDescription ed, Object svc) {
		for (DirectRemoteServiceProvider drsp : this.drsps)
			try {
				switch (type) {
				case EndpointEvent.ADDED:
					drsp.exportService(svc, ed.getProperties());
					break;
				case EndpointEvent.REMOVED:
					drsp.unexportService(ed.getProperties());
					break;
				case EndpointEvent.MODIFIED:
					drsp.updateService(ed.getProperties());
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin;
import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;

@Component(immediate = true)
public class ContainerExporterServiceImpl implements ContainerExporterService, RemoteServiceAdminListener {

	private BundleContext context;
	private List<ServiceReference<DirectRemoteServiceProvider>> drsps = new ArrayList<ServiceReference<DirectRemoteServiceProvider>>();
	private Map<Long, DirectEndpoint> exportedEndpoints = new HashMap<Long, DirectEndpoint>();
	private Map<Long, Object> unexportedEndpoints = new HashMap<Long, Object>();

	private DirectRemoteServiceProvider getDRSP(ServiceReference<DirectRemoteServiceProvider> ref) {
		BundleContext ctxt = this.context;
		return (ctxt != null) ? ctxt.getService(ref) : null;
	}

	@Reference(service = DirectRemoteServiceProvider.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target = "(&(objectClass=org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider)("
			+ DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP + "=*))")
	void bindDirectRemoteServiceProvider(ServiceReference<DirectRemoteServiceProvider> ref) {
		synchronized (this) {
			DirectRemoteServiceProvider p = getDRSP(ref);
			if (p != null)
				for (Long key : exportedEndpoints.keySet()) {
					DirectEndpoint de = exportedEndpoints.get(key);
					if (de != null)
						p.exportService(de.getProxy(), de.getEndpointDescription().getProperties());
				}
			drsps.add(ref);
		}
	}

	void unbindDirectRemoteServiceProvider(ServiceReference<DirectRemoteServiceProvider> p) {
		synchronized (this) {
			drsps.remove(p);
		}
	}

	@Activate()
	void activate(BundleContext ctxt) throws Exception {
		synchronized (this) {
			this.context = ctxt;
		}
	}

	@Deactivate()
	void deactivate(BundleContext ctxt) throws Exception {
		synchronized (this) {
			clear();
			this.context = null;
		}
	}

	@Override
	public void exportFromContainer(long rsvcid, Object proxy) {
		synchronized (this) {
			this.unexportedEndpoints.put(rsvcid, proxy);
		}
	}

	@Override
	public void remoteAdminEvent(RemoteServiceAdminEvent event) {
		if (event instanceof RemoteServiceAdmin.RemoteServiceAdminEvent) {
			RemoteServiceAdmin.RemoteServiceAdminEvent rsaEvent = (RemoteServiceAdmin.RemoteServiceAdminEvent) event;
			EndpointDescription ed = rsaEvent.getEndpointDescription();
			long rsvcid = ed.getRemoteServiceId();
			Object svc = null;
			synchronized (this) {
				switch (event.getType()) {
				case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
					svc = unexportedEndpoints.get(rsvcid);
					if (svc != null) {
						fireServiceChange(EndpointEvent.ADDED, ed, svc);
						unexportedEndpoints.remove(rsvcid);
						exportedEndpoints.put(rsvcid, new DirectEndpoint(ed, svc));
					}
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

	private void fireServiceChange(int type, EndpointDescription ed, Object svc) {
		for (ServiceReference<DirectRemoteServiceProvider> drsp : this.drsps)
			try {
				switch (type) {
				case EndpointEvent.ADDED:
					getDRSP(drsp).exportService(svc, ed.getProperties());
					break;
				case EndpointEvent.REMOVED:
					getDRSP(drsp).unexportService(ed.getProperties());
					break;
				case EndpointEvent.MODIFIED:
					getDRSP(drsp).updateService(ed.getProperties());
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	@Override
	public void clear() {
		synchronized (this) {
			this.drsps.clear();
			this.exportedEndpoints.clear();
			this.unexportedEndpoints.clear();
		}
	}

}

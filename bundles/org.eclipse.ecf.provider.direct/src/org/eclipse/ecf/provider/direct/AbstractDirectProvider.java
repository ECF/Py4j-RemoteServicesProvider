/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDirectProvider
		implements RemoteServiceAdminListener, ExternalDirectDiscovery, InternalServiceProvider {

	private static final Logger logger = LoggerFactory.getLogger(AbstractDirectProvider.class);

	protected static void logError(String message) {
		logError(message, null);
	}

	protected static void logError(String message, Throwable exception) {
		logger.error(message, exception);
	}

	private BundleContext context;

	protected BundleContext getContext() {
		return context;
	}

	protected class ExternalEndpoint {
		private EndpointDescription ed;
		private Object proxy;

		public ExternalEndpoint(EndpointDescription ed, Object proxy) {
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

	private String ecfEndpointListenerScope;
	private List<EndpointEventListener> eels = new ArrayList<EndpointEventListener>();

	protected void bindEndpointEventListener(EndpointEventListener l, @SuppressWarnings("rawtypes") Map props) {
		ecfEndpointListenerScope = ((String[]) props.get("endpoint.listener.scope"))[0];
		synchronized (this.eels) {
			this.eels.add(l);
		}
	}

	protected void unbindEndpointEventListener(EndpointEventListener l) {
		synchronized (this.eels) {
			this.eels.remove(l);
		}
	}

	protected Object getLock() {
		return lock;
	}

	protected abstract String getIdForProxy(Object proxy);

	protected abstract Object removeProxy(String proxyId);

	private void fireEndpointEvent(int type, EndpointDescription ed) {
		List<EndpointEventListener> notifyEels;
		synchronized (this.eels) {
			notifyEels = new ArrayList<EndpointEventListener>(this.eels);
		}
		for (EndpointEventListener l : notifyEels)
			l.endpointChanged(new EndpointEvent(type, ed), ecfEndpointListenerScope);
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void _java_discoverService(Object proxy, Map rsaProps) {
		String proxyId = getIdForProxy(proxy);
		if (proxyId != null) {
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(
					rsaProps, proxyId);
			fireEndpointEvent(EndpointEvent.ADDED, ed);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void _java_undiscoverService(Map rsaProps) {
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		if (ed != null)
			fireEndpointEvent(EndpointEvent.REMOVED, ed);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void _java_updateDiscoveredService(Map rsaProps) {
		org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = createEndpointDescription(rsaProps);
		if (ed != null)
			fireEndpointEvent(EndpointEvent.MODIFIED, ed);
	}

	protected String getProxyId(String edId) {
		synchronized (getLock()) {
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = this.edMap.get(edId);
			return (ed != null) ? (String) ed.getProperties().get(ExternalServiceProvider.PROXYID_PROP_NAME) : null;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription createEndpointDescription(
			Map rsaProps, String proxyId) {
		return new org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription(
				PropertiesUtil.conditionProperties(rsaProps, proxyId));
	}

	protected org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription createEndpointDescription(
			@SuppressWarnings("rawtypes") Map rsaProps) {
		synchronized (getLock()) {
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription existingEd = this.edMap
					.get(rsaProps.get(RemoteConstants.ENDPOINT_ID));
			if (existingEd != null) {
				String proxyId = (String) existingEd.getProperties().get(ExternalServiceProvider.PROXYID_PROP_NAME);
				if (proxyId != null)
					return createEndpointDescription(rsaProps, proxyId);
			}
			return null;
		}
	}

	@Override
	public void remoteAdminEvent(RemoteServiceAdminEvent event) {
		if (event instanceof RemoteServiceAdmin.RemoteServiceAdminEvent) {
			RemoteServiceAdmin.RemoteServiceAdminEvent rsaEvent = (RemoteServiceAdmin.RemoteServiceAdminEvent) event;
			EndpointDescription ed = rsaEvent.getEndpointDescription();
			long rsvcid = ed.getRemoteServiceId();
			Object svc = null;
			synchronized (getLock()) {
				switch (event.getType()) {
				case RemoteServiceAdminEvent.EXPORT_REGISTRATION:
					svc = preexported.get(rsvcid);
					if (svc != null) {
						fireServiceChange(EndpointEvent.ADDED, ed, svc);
						preexported.remove(rsvcid);
						exportedEndpoints.put(rsvcid, new ExternalEndpoint(ed, svc));
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
				case RemoteServiceAdminEvent.IMPORT_REGISTRATION:
					edMap.put(ed.getId(), ed);
					break;
				case RemoteServiceAdminEvent.IMPORT_UNREGISTRATION:
					EndpointDescription oldEd = edMap.remove(ed.getId());
					if (oldEd != null) {
						String proxyId = (String) oldEd.getProperties().get(ExternalServiceProvider.PROXYID_PROP_NAME);
						if (proxyId != null)
							removeProxy(proxyId);
					}
					break;
				case RemoteServiceAdminEvent.IMPORT_UPDATE:
					break;
				default:
					break;
				}
			}
		}
	}

	private Map<String, org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription> edMap = new HashMap<String, org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription>();
	private InternalDirectDiscovery idd;
	private ExternalCallableEndpoint ece;
	private ServiceRegistration<?> iddReg;

	private Map<Long, Object> preexported = new HashMap<Long, Object>();
	private Map<Long, ExternalEndpoint> exportedEndpoints = new HashMap<Long, ExternalEndpoint>();
	private Object lock = new Object();

	public void _setInternalDirectDiscovery(InternalDirectDiscovery directDiscovery, String externalId) {
		// Set internal variable here and export to external any services before
		// returning
		synchronized (getLock()) {
			bindInternalDirectDiscovery(directDiscovery);
			bindExternalCallableEndpoint((ExternalCallableEndpoint) directDiscovery);
		}
		iddReg = getContext().registerService(
				new String[] { InternalDirectDiscovery.class.getName(), ExternalCallableEndpoint.class.getName() },
				directDiscovery, null);
	}

	protected void hardClose() {
		synchronized (getLock()) {
			if (iddReg != null) {
				iddReg.unregister();
				iddReg = null;
			}
			hardCloseExported();
			hardCloseImported();
			unbindExternalCallableEndpoint();
			unbindInternalDirectDiscovery();
		}
	}

	protected void hardCloseImported() {
		List<EndpointDescription> des;
		synchronized (getLock()) {
			des = new ArrayList<EndpointDescription>(this.edMap.values());
		}
		if (des != null)
			for (EndpointDescription ed : des)
				fireEndpointEvent(EndpointEvent.REMOVED, ed);
	}

	protected void hardCloseExported() {
		List<ExternalEndpoint> endpoints;
		synchronized (getLock()) {
			unbindInternalDirectDiscovery();
			endpoints = new ArrayList<ExternalEndpoint>(this.exportedEndpoints.values());
		}
		if (endpoints != null)
			for (ExternalEndpoint ee : endpoints)
				fireEndpointEvent(EndpointEvent.REMOVED, ee.getEndpointDescription());
	}

	protected void bindInternalDirectDiscovery(InternalDirectDiscovery idd) {
		synchronized (getLock()) {
			this.idd = idd;
			for (Long key : exportedEndpoints.keySet()) {
				ExternalEndpoint de = exportedEndpoints.get(key);
				if (de != null)
					fireServiceChange(EndpointEvent.ADDED, de.getEndpointDescription(), de.getProxy());
			}
		}
	}

	protected void unbindInternalDirectDiscovery() {
		synchronized (getLock()) {
			this.idd = null;
		}
	}

	protected InternalDirectDiscovery getInternalDirectDiscovery() {
		synchronized (getLock()) {
			return this.idd;
		}
	}
	
	protected void bindExternalCallableEndpoint(ExternalCallableEndpoint ep) {
		synchronized (getLock()) {
			this.ece = ep;
		}
	}

	protected void unbindExternalCallableEndpoint() {
		synchronized (getLock()) {
			this.ece = null;
		}
	}

	protected ExternalCallableEndpoint getExternalCallableEndpoint() {
		synchronized (getLock()) {
			return this.ece;
		}
	}
	
	protected void activate(BundleContext ctxt) throws Exception {
		this.context = ctxt;
	}

	protected void deactivate(BundleContext ctxt) {
		synchronized (getLock()) {
			hardClose();
			eels.clear();
			this.ecfEndpointListenerScope = null;
			this.context = null;
		}
	}

	private void fireServiceChange(int type, EndpointDescription ed, Object svc) {
		InternalDirectDiscovery intdd;
		synchronized (lock) {
			intdd = this.idd;
		}
		if (intdd != null) {
			switch (type) {
			case EndpointEvent.ADDED:
				intdd._external_discoverService(svc, ed.getProperties());
				break;
			case EndpointEvent.REMOVED:
				intdd._external_undiscoverService(ed.getProperties());
				break;
			case EndpointEvent.MODIFIED:
				intdd._external_updateDiscoveredService(ed.getProperties());
				break;
			}
		}
	}

	/**
	 * Method should be called by external process first thing upon connection. This
	 * will return and impl of ExternalDirectDiscovery interface to allow external
	 * process to export, update, unexport remote services to this Java process for
	 * remote service import,update,unimport. It should not be called by Java code
	 * directly.
	 * 
	 * @return implementation of ExternalDirectDiscovery. Must not return
	 *         <code>null</code>.
	 */
	public ExternalDirectDiscovery _getExternalDirectDiscovery() {
		return this;
	}

	/**
	 * Method to be called by Java code to get access to export,update, unexport
	 * semantics for exported remote services.
	 * 
	 * @return InternalServiceProvider allowing remote serice containers (e.g.
	 *         DirectHostContainer) to communicate to external processes that new
	 *         Java/OSGi remote services are available for use.
	 */
	public InternalServiceProvider getInternalServiceProvider() {
		return this;
	}

	// Implementation of InternalServiceProvider interface. InternalServiceProvider
	// is available for remote service hosts (e.g. DirectHostContainer to
	// use to export remote services.

	/**
	 * Method called to export a OSGi Remote Service.
	 */
	@Override
	public void externalExport(long rsId, Object service) {
		synchronized (this) {
			this.preexported.put(rsId, service);
		}
	}

	public void externalUpdate(long rsId) {
		// No impl now
	}

	public void externalUnexport(long rsId) {
		// No impl now
	}

}

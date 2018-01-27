/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin;
import org.eclipse.ecf.provider.direct.util.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.remoteserviceadmin.EndpointEvent;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminEvent;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract direct provider. This class provides support for creating remote service distribution providers that have
 * direct connections. Py4j provider is such a distribution provider and is based on this class.
 *
 */
public abstract class AbstractDirectProvider
		implements RemoteServiceAdminListener, ExternalDirectDiscovery, InternalServiceProvider, ModuleResolver {

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
		synchronized (getLock()) {
			this.eels.add(l);
		}
	}

	protected void unbindEndpointEventListener(EndpointEventListener l) {
		synchronized (getLock()) {
			this.eels.remove(l);
		}
	}

	protected Object getLock() {
		return lock;
	}

	protected abstract ID getLocalID();

	protected abstract String getIdForProxy(Object proxy);

	protected abstract Object removeProxy(String proxyId);

	private void fireEndpointEvent(int type, EndpointDescription ed) {
		List<EndpointEventListener> notifyEels;
		synchronized (getLock()) {
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
					svc = preExportedServices.get(rsvcid);
					if (svc != null) {
						fireServiceChange(EndpointEvent.ADDED, ed, svc);
						preExportedServices.remove(rsvcid);
						exportedExternalEndpoints.put(rsvcid, new ExternalEndpoint(ed, svc));
					}
					break;
				case RemoteServiceAdminEvent.EXPORT_UNREGISTRATION:
					ExternalEndpoint ee = exportedExternalEndpoints.remove(rsvcid);
					if (ee != null)
						fireServiceChange(EndpointEvent.REMOVED, ee.ed, ee.proxy);
					exportedExternalEndpoints.remove(rsvcid);
					this.preExportedServices.remove(rsvcid);
					break;
				case RemoteServiceAdminEvent.EXPORT_UPDATE:
					svc = exportedExternalEndpoints.get(rsvcid);
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
	protected InternalDirectDiscovery internalDirectDiscovery;
	protected ServiceRegistration<?> internalDirectDiscoverReg;
	protected ExternalCallableEndpoint externalCallbackEndpoint;
	protected ExternalPathProvider externalPathProvider;
	
	protected Map<Long, Object> preExportedServices = new HashMap<Long, Object>();
	private Map<Long, ExternalEndpoint> exportedExternalEndpoints = new HashMap<Long, ExternalEndpoint>();
	private Object lock = new Object();

	protected class DirectBridge implements ExternalPathProvider, InternalDirectDiscovery, ExternalCallableEndpoint {

		private final ExternalPathProvider epp;
		private final InternalDirectDiscovery idd;
		private final ExternalCallableEndpoint ece;

		public DirectBridge(ExternalPathProvider epp, InternalDirectDiscovery d, ExternalCallableEndpoint ce) {
			this.epp = epp;
			this.idd = d;
			this.ece = ce;
		}

		@Override
		public byte[] _call_endpoint(Long rsId, String methodName, byte[] serializedArgs) throws Exception {
			return ece._call_endpoint(rsId, methodName, serializedArgs);
		}

		@Override
		public void _add_code_path(String path) {
			epp._add_code_path(path);
		}

		@Override
		public void _remove_code_path(String path) {
			epp._remove_code_path(path);
		}

		@Override
		public void _external_discoverService(Object service, @SuppressWarnings("rawtypes") Map rsaMap) {
			this.idd._external_discoverService(service, rsaMap);
		}

		@Override
		public void _external_updateDiscoveredService(@SuppressWarnings("rawtypes") Map rsaMap) {
			this.idd._external_undiscoverService(rsaMap);
		}

		@Override
		public void _external_undiscoverService(@SuppressWarnings("rawtypes") Map rsaMap) {
			this.idd._external_updateDiscoveredService(rsaMap);
		}

	}

	public List<String> _setDirectBridge(ExternalPathProvider pathProvider, InternalDirectDiscovery directDiscovery, ExternalCallableEndpoint endpoint,
			String externalId) {
		// Set internal variable here and export to external any services before
		// returning
		DirectBridge external = null;
		List<String> resolverPaths;
		synchronized (getLock()) {
			bindInternalDirectDiscovery(directDiscovery);
			bindExternalCallableEndpoint(endpoint);
			bindExternalPathProvider(pathProvider);
			external = new DirectBridge(pathProvider, directDiscovery, endpoint);
			resolverPaths = getBundleResolverPaths();
		}
		internalDirectDiscoverReg = getContext().registerService(
				new String[] { InternalDirectDiscovery.class.getName(), ExternalCallableEndpoint.class.getName() },
				external, null);
		return resolverPaths;
	}

	protected void hardClose() {
		synchronized (getLock()) {
			if (internalDirectDiscoverReg != null) {
				internalDirectDiscoverReg.unregister();
				internalDirectDiscoverReg = null;
			}
			hardCloseExported();
			hardCloseImported();
			unbindExternalPathProvider();
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
		synchronized (getLock()) {
			unbindInternalDirectDiscovery();
		}
	}

	protected void bindInternalDirectDiscovery(InternalDirectDiscovery idd) {
		synchronized (getLock()) {
			this.internalDirectDiscovery = idd;
			for (Long key : exportedExternalEndpoints.keySet()) {
				ExternalEndpoint de = exportedExternalEndpoints.get(key);
				if (de != null)
					fireServiceChange(EndpointEvent.ADDED, de.getEndpointDescription(), de.getProxy());
			}
		}
	}

	protected void unbindInternalDirectDiscovery() {
		synchronized (getLock()) {
			this.internalDirectDiscovery = null;
		}
	}

	protected InternalDirectDiscovery getInternalDirectDiscovery() {
		synchronized (getLock()) {
			return this.internalDirectDiscovery;
		}
	}

	protected void bindExternalCallableEndpoint(ExternalCallableEndpoint ep) {
		synchronized (getLock()) {
			this.externalCallbackEndpoint = ep;
		}
	}

	protected void unbindExternalCallableEndpoint() {
		synchronized (getLock()) {
			this.externalCallbackEndpoint = null;
		}
	}

	protected void bindExternalPathProvider(ExternalPathProvider epp) {
		synchronized (getLock()) {
			this.externalPathProvider = epp;
		}
	}

	protected void unbindExternalPathProvider() {
		synchronized (getLock()) {
			this.externalPathProvider = null;
		}
	}

	protected ExternalCallableEndpoint getExternalCallableEndpoint() {
		synchronized (getLock()) {
			return this.externalCallbackEndpoint;
		}
	}

	protected ExternalPathProvider getExternalPathProvider() {
		synchronized (getLock()) {
			return this.externalPathProvider;
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
		synchronized (getLock()) {
			intdd = this.internalDirectDiscovery;
		}
		if (intdd != null) {
			Map<String, Object> edmap = ed.getProperties();
			switch (type) {
			case EndpointEvent.ADDED:
				intdd._external_discoverService(svc, edmap);
				break;
			case EndpointEvent.REMOVED:
				intdd._external_undiscoverService(edmap);
				break;
			case EndpointEvent.MODIFIED:
				intdd._external_updateDiscoveredService(edmap);
				break;
			}
		}
	}

	/**
	 * Method should be called by external process first thing upon connection. This will return and impl of
	 * ExternalDirectDiscovery interface to allow external process to export, update, unexport remote services to this
	 * Java process for remote service import,update,unimport. It should not be called by Java code directly.
	 * 
	 * @return implementation of ExternalDirectDiscovery. Must not return <code>null</code>.
	 */
	public ExternalDirectDiscovery _getExternalDirectDiscovery() {
		return this;
	}

	/**
	 * Method to be called by Java code to get access to export,update, unexport semantics for exported remote services.
	 * 
	 * @return InternalServiceProvider allowing remote serice containers (e.g. DirectHostContainer) to communicate to
	 *         external processes that new Java/OSGi remote services are available for use.
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
		synchronized (getLock()) {
			this.preExportedServices.put(rsId, service);
		}
	}

	protected Map<Bundle, List<ServiceReference<ModuleResolver>>> ModuleResolvers = Collections
			.synchronizedMap(new HashMap<Bundle, List<ServiceReference<ModuleResolver>>>());

	protected String convertResolverToPath(Bundle b, ServiceReference<ModuleResolver> ref) {
		return new StringBuffer(getLocalID().getName()).append("/").append(b.getSymbolicName()).append("/")
				.append(b.getVersion().toString()).append("/").append(ref.getProperty(Constants.SERVICE_ID)).append("/")
				.toString();
	}

	protected class ResolverInfo {
		String bundleId;
		String version;
		Long serviceId;
		String remain;
	}

	protected ResolverInfo convertPathToResolverInfo(String path) {
		URI uri = null;
		try {
			uri = new URI(path);
		} catch (URISyntaxException e1) {
			return null;
		}
		String uriPath = uri.getPath();
		while (uriPath.startsWith("/"))
			uriPath = uriPath.substring(1);
		
		String[] parts = uriPath.split("/");
		if (parts.length < 2)
			return null;
		
		ResolverInfo result = new ResolverInfo();
		result.bundleId = parts[1];
		if (parts.length > 2)
			result.version = parts[2];
		if (parts.length > 3)
			try {
				result.serviceId = Long.valueOf(parts[3]);
			} catch (NumberFormatException e) {
				// ignore
			}
		if (parts.length > 4) {
			String[] remainParts = new String[parts.length - 4];
			System.arraycopy(parts, 4, remainParts, 0, remainParts.length);
			StringBuffer buf = new StringBuffer();
			List<String> rps = Arrays.asList(remainParts);
			for (Iterator<String> it=rps.iterator(); it.hasNext(); ) {
				buf.append(it.next());
				if (it.hasNext())
					buf.append("/");
			}
			result.remain = buf.toString();
		}
		return result;
	}

	protected List<String> getBundleResolverPaths() {
		List<String> results = new ArrayList<String>();
		synchronized (ModuleResolvers) {
			for (Bundle b : ModuleResolvers.keySet()) {
				List<ServiceReference<ModuleResolver>> existingList = ModuleResolvers.get(b);
				if (existingList != null)
					for (ServiceReference<ModuleResolver> ref : existingList)
						results.add(convertResolverToPath(b, ref));
			}
		}
		return results;
	}

	protected void fireModuleResolver(ServiceReference<ModuleResolver> ref, boolean add) {
		synchronized (getLock()) {
			ExternalPathProvider epp = getExternalPathProvider();
			if (epp != null) {
				String path = convertResolverToPath(ref.getBundle(), ref);
				if (add)
					epp._add_code_path(path);
				else
					epp._remove_code_path(path);
			}
		}
	}

	protected void bindModuleResolver(ServiceReference<ModuleResolver> ref) {
		Bundle b = ref.getBundle();
		synchronized (ModuleResolvers) {
			List<ServiceReference<ModuleResolver>> existingList = ModuleResolvers.get(b);
			if (existingList == null)
				existingList = new ArrayList<ServiceReference<ModuleResolver>>();
			existingList.add(ref);
			ModuleResolvers.put(b, existingList);
		}
		fireModuleResolver(ref, true);
	}

	protected void unbindModuleResolver(ServiceReference<ModuleResolver> ref) {
		Bundle b = ref.getBundle();
		synchronized (ModuleResolvers) {
			List<ServiceReference<ModuleResolver>> existingList = ModuleResolvers.get(b);
			if (existingList != null)
				existingList.remove(ref);
			if (existingList.size() == 0)
				ModuleResolvers.remove(b);
		}
		fireModuleResolver(ref, false);
	}

	protected ServiceReference<ModuleResolver> findModuleResolverRef(ResolverInfo info) {
		BundleContext ctx = this.context;
		if (info == null || ctx == null)
			return null;
		Bundle bundle = null;
		String bundleVersion = info.version;
		for (Bundle b : ctx.getBundles())
			if (b.getSymbolicName().equals(info.bundleId) && (bundleVersion == null
					|| b.getVersion().toString().equals(bundleVersion) || Version.emptyVersion.equals(bundleVersion))) {
				bundle = b;
				break;
			}
		if (bundle != null)
			synchronized (this.ModuleResolvers) {
				List<ServiceReference<ModuleResolver>> existingList = ModuleResolvers.get(bundle);
				if (existingList != null) {
					for (ServiceReference<ModuleResolver> ref : existingList) {
						Long infoServiceId = info.serviceId;
						if (infoServiceId != null && infoServiceId.equals(ref.getProperty(Constants.SERVICE_ID)))
							return ref;
					}
				}
			}
		return null;
	}

	@Override
	public int getModuleType(String moduleUri) {
		if (moduleUri != null) {
			ResolverInfo resolverInfo = convertPathToResolverInfo(moduleUri);
			if (resolverInfo != null) {
				ServiceReference<ModuleResolver> ref = findModuleResolverRef(resolverInfo);
				if (ref != null) {
					BundleContext bc = this.context;
					if (bc != null) {
						ModuleResolver bmr = bc.getService(ref);
						if (bmr != null) {
							int result = bmr.getModuleType(resolverInfo.remain);
							bc.ungetService(ref);
							return result;
						}
					}
				}
			}
		}
		return ModuleResolver.NONE;
	}

	@Override
	public String getModuleCode(String moduleUri, boolean ispackage) throws Exception {
		if (moduleUri != null) {
			ResolverInfo resolverInfo = convertPathToResolverInfo(moduleUri);
			if (resolverInfo != null) {
				ServiceReference<ModuleResolver> ref = findModuleResolverRef(resolverInfo);
				if (ref != null) {
					BundleContext bc = this.context;
					if (bc != null) {
						ModuleResolver bmr = bc.getService(ref);
						if (bmr != null) {
							String result = bmr.getModuleCode(resolverInfo.remain,ispackage);
							bc.ungetService(ref);
							return result;
						}
					}
				}
			}
		}
		return null;
	}
}

/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.internal.py4j;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.eclipse.ecf.provider.direct.local.ContainerExporterService;
import org.eclipse.ecf.provider.direct.local.ProxyMapperService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

@Component(immediate = true)
public class RSAComponent {

	private static final boolean GATEWAY_DEBUG = new Boolean(
			System.getProperty("org.eclipse.provider.direct.gateway.debug", "true")).booleanValue();

	private BundleContext context;
	private GatewayServer gateway;
	private ProxyMapperService javaConsumer;

	private static RSAComponent instance;

	private List<Py4JServerConnection> py4jConnections = new ArrayList<Py4JServerConnection>();
	private ServiceRegistration<DirectRemoteServiceProvider> drspReg;

	private ContainerExporterService containerExporterService;

	public static RSAComponent getDefault() {
		return instance;
	}

	public RSAComponent() {
		instance = this;
	}

	@Reference(service = ProxyMapperService.class)
	void bindLocalProxyMapper(ProxyMapperService pmm) {
		synchronized (this) {
			this.javaConsumer = pmm;
		}
	}

	void unbindLocalProxyMapper(ProxyMapperService pmm) {
		pmm.clear();
		synchronized (this) {
			this.javaConsumer = null;
		}
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	void bindContainerExporter(ContainerExporterService e) {
		this.containerExporterService = e;
	}

	void unbindContainerExporter(ContainerExporterService e) {
		this.containerExporterService = null;
	}

	public ContainerExporterService getContainerExporter() {
		return this.containerExporterService;
	}

	private GatewayServerListener gatewayServerListener = new GatewayServerListener() {

		@Override
		public void connectionError(Exception arg0) {
		}

		@Override
		public void connectionStarted(Py4JServerConnection arg0) {
			synchronized (RSAComponent.this) {
				RSAComponent.this.py4jConnections.add(arg0);
			}
		}

		@Override
		public void connectionStopped(Py4JServerConnection arg0) {
			synchronized (RSAComponent.this) {
				if (RSAComponent.this.py4jConnections.remove(arg0))
					hardClose();
			}
		}

		@Override
		public void serverError(Exception arg0) {
		}

		@Override
		public void serverPostShutdown() {
		}

		@Override
		public void serverPreShutdown() {
		}

		@Override
		public void serverStarted() {
		}

		@Override
		public void serverStopped() {
			RSAComponent.this.restartGateway();
		}
	};

	public ProxyMapperService getProxyMapper() {
		return javaConsumer;
	}

	private void hardClose() {
		synchronized (this) {
			if (drspReg != null) {
				drspReg.unregister();
				drspReg = null;
			}
			if (javaConsumer != null)
				javaConsumer.clear();
			py4jConnections.clear();
		}
	}

	@SuppressWarnings("unchecked")
	public void setPythonConsumer(DirectRemoteServiceProvider consumer) {
		synchronized (this) {
			if (context != null && py4jConnections.size() > 0) {
				@SuppressWarnings("rawtypes")
				Hashtable ht = new Hashtable();
				ht.put(DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP, "python." + this.gateway.getPythonPort());
				drspReg = context.registerService(DirectRemoteServiceProvider.class, consumer, ht);
			}
		}
	}

	public DirectRemoteServiceProvider getJavaConsumer() {
		return javaConsumer;
	}

	public String getGatewayHostAddress() {
		synchronized (this) {
			if (gateway == null)
				throw new NullPointerException("Gateway host cannot be accessed");
			return gateway.getAddress().getHostAddress();
		}
	}

	public int getGatewayConnectTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConnectTimeout();
		}
	}

	public int getGatewayReadTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getReadTimeout();
		}
	}

	public int getGatewayPort() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getListeningPort();
		}
	}

	@Activate
	void activate(BundleContext ctxt) throws Exception {
		synchronized (this) {
			context = ctxt;
			drspReg = null;
			startGateway();
		}
	}

	void startGateway() {
		synchronized (this) {
			if (GATEWAY_DEBUG)
				GatewayServer.turnAllLoggingOn();
			gateway = new GatewayServer(this);
			gateway.addListener(gatewayServerListener);
			gateway.start();
		}
	}

	void stopGateway() {
		synchronized (this) {
			if (gateway != null) {
				gateway.removeListener(gatewayServerListener);
				gateway.shutdown();
				gateway = null;
			}
		}
	}

	void restartGateway() {
		synchronized (this) {
			stopGateway();
			startGateway();
		}
	}

	@Deactivate
	void deactivate() throws Exception {
		synchronized (this) {
			hardClose();
			stopGateway();
		}
		context = null;
	}

}

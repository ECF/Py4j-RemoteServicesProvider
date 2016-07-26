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
import org.osgi.service.component.annotations.ReferencePolicy;
import org.py4j.osgi.GatewayConfiguration;
import org.py4j.osgi.IGateway;
import org.py4j.osgi.IGatewayConfiguration;

import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

@Component(immediate = true)
public class RSAComponent {

	private BundleContext context;
	private ProxyMapperService javaConsumer;

	private static RSAComponent instance;

	private List<Py4JServerConnection> py4jConnections = new ArrayList<Py4JServerConnection>();
	private ServiceRegistration<DirectRemoteServiceProvider> drspReg;

	private ContainerExporterService containerExporterService;

	private IGateway gateway;
	
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

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	void bindGateway(IGateway gateway) {
		this.gateway = gateway;
	}
	
	void unbindGateway(IGateway gateway) {
		this.gateway = null;
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
				ht.put(DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP, "python." + this.gateway.getConfiguration().getPythonPort());
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
			return gateway.getConfiguration().getAddress().getHostAddress();
		}
	}

	public int getGatewayConnectTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getConnectTimeout();
		}
	}

	public int getGatewayReadTimeout() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getReadTimeout();
		}
	}

	public int getGatewayPort() {
		synchronized (this) {
			if (gateway == null)
				return -1;
			return gateway.getConfiguration().getListeningPort();
		}
	}

	private ServiceRegistration<IGatewayConfiguration> gwConfigReg = null;
	@Activate
	void activate(BundleContext ctxt) throws Exception {
		IGatewayConfiguration config = new GatewayConfiguration.Builder(this).addGatewayServerListener(gatewayServerListener).setUseLoadingBundleClassLoadingStrategy(true).build();
		synchronized (this) {
			context = ctxt;
			drspReg = null;
			gwConfigReg = context.registerService(IGatewayConfiguration.class, config, null);
		}
	}

	@Deactivate
	void deactivate() throws Exception {
		synchronized (this) {
			hardClose();
			if (gwConfigReg != null) {
				gwConfigReg.unregister();
				gwConfigReg = null;
			}
		}
		context = null;
	}

}

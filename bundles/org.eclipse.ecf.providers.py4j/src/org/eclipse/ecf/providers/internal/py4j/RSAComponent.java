/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.providers.internal.py4j;

import java.util.Hashtable;

import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.eclipse.ecf.provider.direct.local.ContainerExporter;
import org.eclipse.ecf.provider.direct.local.LocalProxyMapper;
import org.osgi.framework.BundleContext;
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

	private BundleContext context;
	private GatewayServer gateway;
	private DirectRemoteServiceProvider pythonConsumer;
	private LocalProxyMapper javaConsumer;

	private static RSAComponent instance;

	public static RSAComponent getDefault() {
		return instance;
	}

	public RSAComponent() {
		instance = this;
	}

	@Reference(service = LocalProxyMapper.class)
	void bindLocalProxyMapper(LocalProxyMapper pmm) {
		synchronized (this) {
			this.javaConsumer = pmm;
		}
	}

	void unbindLocalProxyMapper(LocalProxyMapper pmm) {
		pmm.clear();
		synchronized (this) {
			this.javaConsumer = null;
		}
	}

	private ContainerExporter containerExporter;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY)
	void bindContainerExporter(ContainerExporter e) {
		this.containerExporter = e;
	}
	
	void unbindContainerExporter(ContainerExporter e) {
		this.containerExporter = null;
	}
	
	public ContainerExporter getContainerExporter() {
		return this.containerExporter;
	}
	
	private GatewayServerListener gatewayServerListener = new GatewayServerListener() {

		@Override
		public void connectionError(Exception arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void connectionStarted(Py4JServerConnection arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void connectionStopped(Py4JServerConnection arg0) {
			synchronized (RSAComponent.this) {
				if (javaConsumer != null)
					javaConsumer.clear();
			}
		}

		@Override
		public void serverError(Exception arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void serverPostShutdown() {
			// TODO Auto-generated method stub

		}

		@Override
		public void serverPreShutdown() {
			// TODO Auto-generated method stub

		}

		@Override
		public void serverStarted() {
			// TODO Auto-generated method stub

		}

		@Override
		public void serverStopped() {
			// TODO Auto-generated method stub

		}

	};

	public LocalProxyMapper getProxyMapper() {
		System.out.println("getProxyMapper");
		return javaConsumer;
	}

	@SuppressWarnings("unchecked")
	public void setPythonConsumer(DirectRemoteServiceProvider consumer) {
		if (context != null) {
			System.out.println("setPythonConsumer");
			pythonConsumer = consumer;
			@SuppressWarnings("rawtypes")
			Hashtable ht = new Hashtable();
			ht.put(DirectRemoteServiceProvider.EXTERNAL_SERVICE_PROP, "python." + this.gateway.getPythonPort());
			context.registerService(DirectRemoteServiceProvider.class, consumer, ht);
		}
	}

	public DirectRemoteServiceProvider getPythonConsumer() {
		return pythonConsumer;
	}

	public DirectRemoteServiceProvider getJavaConsumer() {
		return javaConsumer;
	}

	public int getPort() {
		if (gateway == null)
			return -1;
		return gateway.getPort();
	}

	@Activate
	void activate(BundleContext ctxt) throws Exception {
		context = ctxt;
		GatewayServer.turnAllLoggingOn();
		gateway = new GatewayServer(this);
		gateway.addListener(gatewayServerListener);
		gateway.start();
	}

	@Deactivate
	void deactivate() throws Exception {
		if (gateway != null) {
			gateway.removeListener(gatewayServerListener);
			pythonConsumer = null;
			gateway.shutdown();
			gateway = null;
			context = null;
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j;

import java.util.Map;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.provider.direct.AbstractDirectProvider;
import org.eclipse.ecf.provider.direct.DirectClientContainer;
import org.eclipse.ecf.provider.direct.DirectHostContainer;
import org.eclipse.ecf.provider.direct.DirectRemoteServiceDistributionProvider;
import org.eclipse.ecf.provider.direct.ExternalServiceProvider;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.py4j.osgi.GatewayServer;
import org.py4j.osgi.GatewayServerConfiguration;
import org.py4j.osgi.OSGiGateway;

import py4j.CallbackClient;
import py4j.GatewayServerListener;
import py4j.Py4JPythonClient;
import py4j.Py4JServerConnection;

public class Py4jDirectProviderImpl extends AbstractDirectProvider
		implements RemoteServiceAdminListener, Py4jDirectProvider {

	protected static final String[] py4jSupportedIntents = { "passByReference", "exactlyOnce", "ordered" };

	protected void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	protected void unbindEndpointEventListener(EndpointEventListener eel) {
		super.unbindEndpointEventListener(eel);
	}

	private DirectProviderGateway osgiGateway;
	private GatewayServer gatewayServer;
	private Py4JServerConnection connection;

	protected ServiceRegistration<IRemoteServiceDistributionProvider> hostReg;
	protected ServiceRegistration<IRemoteServiceDistributionProvider> clientReg;

	protected ExternalServiceProvider getServiceProxyProvider() {
		synchronized (getLock()) {
			return this.osgiGateway;
		}
	}

	protected class DirectProviderGateway extends OSGiGateway implements ExternalServiceProvider {
		public DirectProviderGateway(Object entryPoint, Py4JPythonClient cbClient) {
			super(entryPoint, cbClient);
		}
	}

	@Override
	protected String getIdForProxy(Object proxy) {
		synchronized (getLock()) {
			return (osgiGateway == null) ? null : osgiGateway.getIdForProxy(proxy);
		}
	}

	@Override
	protected Object removeProxy(String proxyId) {
		synchronized (getLock()) {
			return (osgiGateway == null) ? null : osgiGateway.removeProxy(proxyId);
		}
	}

	private GatewayServerListener gatewayServerListener = new GatewayServerListener() {

		@Override
		public void connectionError(Exception arg0) {
			logError("Connection error", arg0);
		}

		@Override
		public void connectionStarted(Py4JServerConnection arg0) {
			synchronized (getLock()) {
				if (Py4jDirectProviderImpl.this.connection != null)
					logError("connectionStarted error: Already have connection="
							+ Py4jDirectProviderImpl.this.connection + ".  New connectionStarted=" + arg0);
				Py4jDirectProviderImpl.this.connection = arg0;
			}
		}

		@Override
		public void connectionStopped(Py4JServerConnection arg0) {
			synchronized (getLock()) {
				if (Py4jDirectProviderImpl.this.connection == null)
					logError("connectionStopped error: this.connection already null");
				else if (Py4jDirectProviderImpl.this.connection != arg0)
					logError("connectionStopped error: this.connection=" + Py4jDirectProviderImpl.this.connection
							+ " not equal to arg0=" + arg0);
				else {
					hardClose();
				}
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

	@Override
	protected void hardClose() {
		synchronized (getLock()) {
			super.hardClose();
			this.connection = null;
			if (this.osgiGateway != null) {
				this.osgiGateway.reset();
			}
		}
	}

	public static final String PYTHON_PORT_PROP = "pythonPort";
	public static final String PORT_PROP = "port";
	public static final String DEBUG_PROP = "debug";

	private Integer getIntProperty(String name, Map<String, ?> properties, Integer def) {
		Object o = properties.get(name);
		if (o instanceof Integer)
			return (Integer) o;
		else if (o instanceof String) {
			try {
				return Integer.valueOf((String) o);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}

	protected boolean getBooleanProperty(String name, Map<String, ?> properties, Boolean def) {
		Object o = properties.get(name);
		if (o instanceof Boolean)
			return (Boolean) o;
		else if (o instanceof String)
			return Boolean.valueOf((String) o);
		return def;
	}

	protected void registerDistributionProviders() {
		hostReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceDistributionProvider(Py4jConstants.JAVA_HOST_CONFIG_TYPE,
						new RemoteServiceContainerInstantiator(Py4jConstants.JAVA_HOST_CONFIG_TYPE,
								Py4jConstants.JAVA_HOST_CONFIG_TYPE) {
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								GatewayServerConfiguration config = gatewayServer.getConfiguration();
								return new DirectHostContainer(Py4jNamespace
										.createPy4jID(config.getAddress().getHostAddress(), config.getPort()),
										getInternalServiceProvider());
							}

							@Override
							public String[] getSupportedIntents(ContainerTypeDescription description) {
								return py4jSupportedIntents;
							}
						}, true),
				null);

		clientReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceDistributionProvider(Py4jConstants.JAVA_CONSUMER_CONFIG_TYPE,
						new RemoteServiceContainerInstantiator(Py4jConstants.PYTHON_HOST_CONFIG_TYPE,
								Py4jConstants.JAVA_CONSUMER_CONFIG_TYPE) {
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								return new DirectClientContainer(Py4jNamespace.createUUID(), getServiceProxyProvider());
							}

							public String[] getSupportedIntents(ContainerTypeDescription description) {
								return py4jSupportedIntents;
							}
						}, false),
				null);
	}

	protected void activate(BundleContext context, Map<String, ?> properties) throws Exception {
		super.activate(context);
		Integer pythonPort = getIntProperty(PYTHON_PORT_PROP, properties, py4j.GatewayServer.DEFAULT_PYTHON_PORT);
		Integer port = getIntProperty(PORT_PROP, properties, py4j.GatewayServer.DEFAULT_PORT);
		boolean debug = getBooleanProperty(DEBUG_PROP, properties, true);
		synchronized (getLock()) {
			this.osgiGateway = new DirectProviderGateway(this, new CallbackClient(pythonPort));
			GatewayServerConfiguration.Builder builder = new GatewayServerConfiguration.Builder(this).setPort(port)
					.setGateway(this.osgiGateway).addGatewayServerListener(gatewayServerListener)
					.setClassLoadingStrategyBundles(new Bundle[] { context.getBundle() }).setDebug(debug);
			this.gatewayServer = new GatewayServer(builder.build());
			registerDistributionProviders();
		}
	}

	protected void deactivate() {
		synchronized (getLock()) {
			super.deactivate(getContext());
			if (hostReg != null) {
				hostReg.unregister();
				hostReg = null;
			}
			if (clientReg != null) {
				clientReg.unregister();
				clientReg = null;
			}
			this.osgiGateway = null;
			if (this.gatewayServer != null) {
				this.gatewayServer.shutdown();
				this.gatewayServer = null;
			}
		}
	}

	@Override
	public boolean isConnected() {
		synchronized (getLock()) {
			return (this.connection != null) ? true : false;
		}
	}

	@Override
	public int getPythonPort() {
		synchronized (getLock()) {
			return (this.gatewayServer == null) ? -1 : this.gatewayServer.getConfiguration().getPythonPort();
		}
	}

	@Override
	public int getJavaPort() {
		synchronized (getLock()) {
			return (this.gatewayServer == null) ? -1 : this.gatewayServer.getConfiguration().getPort();
		}
	}

}

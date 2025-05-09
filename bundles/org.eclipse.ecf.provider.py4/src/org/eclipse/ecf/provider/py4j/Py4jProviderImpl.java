/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.AbstractDirectProvider;
import org.eclipse.ecf.provider.direct.ExternalServiceProvider;
import org.eclipse.ecf.provider.direct.util.DirectClientContainer;
import org.eclipse.ecf.provider.direct.util.DirectHostContainer;
import org.eclipse.ecf.provider.direct.util.DirectRemoteServiceClientDistributionProvider;
import org.eclipse.ecf.provider.direct.util.DirectRemoteServiceHostDistributionProvider;
import org.eclipse.ecf.provider.direct.util.IDirectContainerInstantiator;
import org.eclipse.ecf.provider.direct.util.PropertiesUtil;
import org.eclipse.ecf.provider.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdminListener;
import org.py4j.osgi.GatewayServer;
import org.py4j.osgi.GatewayServerConfiguration;
import org.py4j.osgi.OSGiGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py4j.CallbackClient;
import py4j.GatewayServerListener;
import py4j.Py4JPythonClient;
import py4j.Py4JServerConnection;

/**
 * Py4j-based remote service distribution provider.
 * 
 * @author slewis
 *
 */
public class Py4jProviderImpl extends AbstractDirectProvider implements RemoteServiceAdminListener, Py4jProvider {

	private static Logger logger = LoggerFactory.getLogger(Py4jProviderImpl.class);
	
	@SuppressWarnings("rawtypes")
	public static Map getPropsFromReference(ServiceReference<?> sr) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : sr.getPropertyKeys())
			result.put(key, sr.getProperty(key));
		return result;
	}

	protected static final String[] py4jSupportedIntents = { "passByReference", "exactlyOnce", "ordered", "py4j",
			"py4j.async", "osgi.async", "osgi.private", "osgi.confidential" };

	protected ID localId;

	protected ID getLocalID() {
		synchronized (getLock()) {
			return localId;
		}
	}

	protected void setLocalId(String address, int port) {
		synchronized (getLock()) {
			localId = Py4jNamespace.createPy4jID(address, port);
		}
	}

	public void bindEndpointEventListener(EndpointEventListener eel, @SuppressWarnings("rawtypes") Map props) {
		super.bindEndpointEventListener(eel, props);
	}

	public void unbindEndpointEventListener(EndpointEventListener eel) {
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
		public void connectionError(Exception except) {
			logError("Connection error", except);
		}

		@Override
		public void connectionStarted(Py4JServerConnection connection) {
			synchronized (getLock()) {
				if (Py4jProviderImpl.this.connection != null) {
					logError("connectionStarted error: Already have connection on port=" + String.valueOf(connection.getSocket().getLocalPort()));
				}
				Py4jProviderImpl.this.connection = connection;
			}
			logger.debug("connectionStarted port=" + String.valueOf(connection.getSocket().getLocalPort()));
		}

		@Override
		public void connectionStopped(Py4JServerConnection connection) {
			hardClose();
			logger.debug("connectionStopped port=" + String.valueOf(connection.getSocket().getLocalPort()));
		}

		@Override
		public void serverError(Exception arg0) {
			logger.error("gateway error",arg0);
		}

		@Override
		public void serverPostShutdown() {
		}

		@Override
		public void serverPreShutdown() {
		}

		@Override
		public void serverStarted() {
			logger.debug("gateway started");
		}

		@Override
		public void serverStopped() {
			logger.debug("gateway stopped");
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

	protected void registerHostDistributionProvider() {
		hostReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceHostDistributionProvider(Py4jConstants.JAVA_HOST_CONFIG_TYPE,
						Py4jConstants.PYTHON_CONSUMER_CONFIG_TYPE, new IDirectContainerInstantiator() {
							@Override
							public IContainer createContainer() throws ContainerCreateException {
								ID lId = Py4jProviderImpl.this.localId;
								if (lId == null)
									throw new ContainerCreateException("Cannot create container with null localId");
								return new DirectHostContainer(lId, getInternalServiceProvider());
							}
						}, py4jSupportedIntents),
				null);
	}

	protected void registerClientDistributionProvider() {
		clientReg = getContext().registerService(IRemoteServiceDistributionProvider.class,
				new DirectRemoteServiceClientDistributionProvider(Py4jConstants.JAVA_CONSUMER_CONFIG_TYPE,
						Py4jConstants.PYTHON_HOST_CONFIG_TYPE, new IDirectContainerInstantiator() {
							@Override
							public IContainer createContainer() throws ContainerCreateException {
								return new DirectClientContainer(Py4jNamespace.createUUID(), getServiceProxyProvider());
							}
						}, py4jSupportedIntents),
				null);
	}

	private static final String PP = Py4jProvider.class.getName() + ".";
	public static final String PYTHON_PORT_PROP = "pythonPort";
	public static final String PYTHON_PORT_SYSPROP = PP + PYTHON_PORT_PROP;
	public static final String JAVA_PORT_PROP = "port";
	public static final String JAVA_PORT_SYSPROP = PP + JAVA_PORT_PROP;
	public static final String ADDRESS_PROP = "address";
	public static final String ADDRESS_SYSPROP = PP + ADDRESS_PROP;
	public static final String DEBUG_PROP = "debug";
	public static final String DEBUG_SYSPROP = PP + DEBUG_PROP;
	public static final String READ_TIMEOUT_PROP = "readTimeout";
	public static final String READ_TIMEOUT_SYSPROP = PP + READ_TIMEOUT_PROP;
	public static final String CONNECT_TIMEOUT_PROP = "connectTimeout";
	public static final String CONNECT_TIMEOUT_SYSPROP = PP + CONNECT_TIMEOUT_PROP;
	public static final String MINCONNECTION_TIME_PROP = "minConnectionTime";
	public static final String MINCONNECTION_TIME_SYSPROP = PP + MINCONNECTION_TIME_PROP;

	public @interface Config {
		int pythonPort() default 25334;

		int port() default 25333;

		String address() default "127.0.0.1";

		boolean debug() default false;

		int readTimeout() default 0;

		int connectTimeout() default 0;

		int minConnectionTime() default 0;
	}

	protected GatewayServerConfiguration.Builder createGatewayServerConfigurationBuilder(int javaPort, String address,
			boolean debug, int readTimeout, int connectTimeout) throws Exception {
		return new GatewayServerConfiguration.Builder(this).setPort(javaPort).setGateway(this.osgiGateway)
				.setConnectTimeout(connectTimeout).setReadTimeout(readTimeout)
				.setAddress(InetAddress.getByName(address)).addGatewayServerListener(gatewayServerListener)
				.setClassLoadingStrategyBundles(new Bundle[] { getContext().getBundle() }).setDebug(debug);
	}

	public void activate(BundleContext context, Map<String, ?> properties) throws Exception {
		super.activate(context);
		Integer pythonPort = PropertiesUtil.getIntValue(PYTHON_PORT_SYSPROP, properties, PYTHON_PORT_PROP,
				py4j.GatewayServer.DEFAULT_PYTHON_PORT);
		Integer javaPort = PropertiesUtil.getIntValue(JAVA_PORT_SYSPROP, properties, JAVA_PORT_PROP,
				py4j.GatewayServer.DEFAULT_PORT);
		Boolean debug = PropertiesUtil.getBooleanValue(DEBUG_SYSPROP, properties, DEBUG_PROP, false);
		String address = PropertiesUtil.getStringValue(ADDRESS_SYSPROP, properties, ADDRESS_PROP,
				py4j.GatewayServer.DEFAULT_ADDRESS);
		Integer readTimeout = PropertiesUtil.getIntValue(READ_TIMEOUT_SYSPROP, properties, READ_TIMEOUT_PROP,
				py4j.GatewayServer.DEFAULT_READ_TIMEOUT);
		Integer connectTimeout = PropertiesUtil.getIntValue(CONNECT_TIMEOUT_SYSPROP, properties, CONNECT_TIMEOUT_PROP,
				py4j.GatewayServer.DEFAULT_CONNECT_TIMEOUT);
		Integer minConnectionTime = PropertiesUtil.getIntValue(MINCONNECTION_TIME_SYSPROP, properties,
				MINCONNECTION_TIME_PROP, 0);
		doActivate(pythonPort, javaPort, debug, address, readTimeout, connectTimeout, minConnectionTime);
	}

	protected void createAndStartGatewayServer(GatewayServerConfiguration serverConfiguration) throws Exception {
		this.gatewayServer = new GatewayServer(serverConfiguration);
	}

	protected void createOSGiGateway(int pythonPort, String address, int minConnectionTime) throws Exception {
		this.osgiGateway = new DirectProviderGateway(this,
				new CallbackClient(pythonPort, InetAddress.getByName(address), minConnectionTime, TimeUnit.SECONDS));
	}

	private void doActivate(int pythonPort, int javaPort, Boolean debug, String address, int readTimeout, int connectTimeout,
			int minConnectTime) throws Exception {
		synchronized (getLock()) {
			createOSGiGateway(pythonPort, address, minConnectTime);
			GatewayServerConfiguration.Builder builder = createGatewayServerConfigurationBuilder(javaPort, address,
					debug, readTimeout, connectTimeout);
			logger.debug("starting gateway server javaPort=" + String.valueOf(javaPort) + ";pythonPort=" + String.valueOf(pythonPort) + ";address="
					+ address + ";connectTimeout=" + String.valueOf(connectTimeout) + ";readTimeout="
					+ String.valueOf(readTimeout) + ";minConnectTime=" + String.valueOf(minConnectTime));
			createAndStartGatewayServer(builder.build());
			setLocalId(address, javaPort);
			registerHostDistributionProvider();
			registerClientDistributionProvider();
		}
	}

	protected void activate(BundleContext context, Config config) throws Exception {
		super.activate(context);
		Integer pythonPort = config.pythonPort();
		Integer javaPort = config.port();
		String address = config.address();
		Boolean debug = config.debug();
		Integer readTimeout = config.readTimeout();
		Integer connectTimeout = config.connectTimeout();
		Integer minConnectTime = config.minConnectionTime();
		doActivate(pythonPort, javaPort, debug, address, readTimeout, connectTimeout, minConnectTime);
	}

	public void deactivate() {
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
			return (this.gatewayServer == null) ? -1 : this.gatewayServer.getConfiguration().getListeningPort();
		}
	}

	public String getPythonAddress() {
		synchronized (getLock()) {
			return (this.gatewayServer == null) ? null
					: this.gatewayServer.getConfiguration().getPythonAddress().getHostAddress();
		}
	}

	public String getJavaAddress() {
		synchronized (getLock()) {
			return (this.gatewayServer == null) ? null
					: this.gatewayServer.getConfiguration().getAddress().getHostAddress();
		}
	}

}

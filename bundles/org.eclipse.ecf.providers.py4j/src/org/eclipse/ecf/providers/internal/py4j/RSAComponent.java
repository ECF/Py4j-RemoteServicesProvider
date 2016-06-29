/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.providers.internal.py4j;

import org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider;
import org.eclipse.ecf.provider.direct.local.JavaDirectRemoteServiceProvider;
import org.eclipse.ecf.provider.direct.local.ProxyMapper;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;

import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

@Component(immediate = true)
public class RSAComponent {

	private static BundleContext context;
	private static RemoteServiceAdmin rsa;
	private static GatewayServer gateway;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	void bindRemoteServiceAdmin(RemoteServiceAdmin r) {
		rsa = r;
	}

	void unbindRemoteServiceAdmin(RemoteServiceAdmin r) {
		rsa = null;
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
			if (javaConsumer != null)
				javaConsumer.clear();
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

	private static JavaDirectRemoteServiceProvider javaConsumer;
	private static DirectRemoteServiceProvider pythonConsumer;

	public static ProxyMapper getJavaConsumer() {
		System.out.println("getJavaConsumer");
		return javaConsumer;
	}

	public static void setPythonConsumer(DirectRemoteServiceProvider consumer) {
		if (context != null) {
			System.out.println("setPythonConsumer");
			pythonConsumer = consumer;
			context.registerService(DirectRemoteServiceProvider.class, consumer, null);
		}
	}

	public static DirectRemoteServiceProvider getPythonConsumer() {
		return pythonConsumer;
	}

	public static int getPort() {
		if (gateway == null) return -1;
		return gateway.getPort();
	}
	
	@Activate
	void activate(BundleContext ctxt) throws Exception {
		context = ctxt;
		javaConsumer = new JavaDirectRemoteServiceProvider(rsa);
		gateway = new GatewayServer(this);
		gateway.addListener(gatewayServerListener);
		gateway.start();
	}

	@Deactivate
	void deactivate() throws Exception {
		if (gateway != null) {
			gateway.removeListener(gatewayServerListener);
			javaConsumer.clear();
			javaConsumer = null;
			pythonConsumer = null;
			gateway.shutdown();
			gateway = null;
			context = null;
		}
	}

}

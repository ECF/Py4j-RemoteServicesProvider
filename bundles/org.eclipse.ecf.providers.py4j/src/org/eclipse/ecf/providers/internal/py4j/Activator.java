/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.providers.internal.py4j;

import java.util.Map;
import java.util.UUID;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.provider.direct.local.DirectClientContainer;
import org.eclipse.ecf.provider.direct.local.DirectHostContainer;
import org.eclipse.ecf.providers.py4j.Py4jConstants;
import org.eclipse.ecf.providers.py4j.identity.Py4jNamespace;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(Namespace.class, new Py4jNamespace(), null);
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(Py4jConstants.JAVA_HOST_CONFIG_TYPE)
						.setInstantiator(new RemoteServiceContainerInstantiator(Py4jConstants.JAVA_HOST_CONFIG_TYPE,
								Py4jConstants.JAVA_HOST_CONFIG_TYPE) {
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								return new DirectHostContainer(
										Py4jNamespace.INSTANCE.createInstance(new Object[] {
												"http://localhost:" + RSAComponent.getDefault().getPort() + ":/java" }),
										RSAComponent.getDefault().getContainerExporter());
							}
						}).setServer(true).setHidden(false).build(),
				null);
		context.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(Py4jConstants.JAVA_CONSUMER_CONFIG_TYPE)
						.setInstantiator(new RemoteServiceContainerInstantiator(Py4jConstants.PYTHON_HOST_CONFIG_TYPE,
								Py4jConstants.JAVA_CONSUMER_CONFIG_TYPE) {
							public IContainer createInstance(ContainerTypeDescription description,
									Map<String, ?> parameters) {
								ID clientID = Py4jNamespace.getInstance()
										.createInstance(new Object[] { "uuid:" + UUID.randomUUID().toString() });
								return new DirectClientContainer(clientID, RSAComponent.getDefault().getProxyMapper());
							}
						}).setServer(false).setHidden(false).build(),
				null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}

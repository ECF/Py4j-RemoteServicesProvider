/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.util.Dictionary;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.remoteservice.provider.AdapterConfig;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;

public class DirectRemoteServiceDistributionProvider implements IRemoteServiceDistributionProvider {

	@Override
	public ContainerTypeDescription getContainerTypeDescription() {
		return provider.getContainerTypeDescription();
	}

	@Override
	public Dictionary<String, ?> getContainerTypeDescriptionProperties() {
		return provider.getContainerTypeDescriptionProperties();
	}

	@Override
	public Namespace getNamespace() {
		return provider.getNamespace();
	}

	@Override
	public Dictionary<String, ?> getNamespaceProperties() {
		return provider.getNamespaceProperties();
	}

	@Override
	public AdapterConfig[] getAdapterConfigs() {
		return provider.getAdapterConfigs();
	}

	private IRemoteServiceDistributionProvider provider;

	protected void setProvider(String name, RemoteServiceContainerInstantiator inst, boolean server, boolean hidden) {
		this.provider = new RemoteServiceDistributionProvider.Builder().setName(name).setInstantiator(inst)
				.setServer(server).setHidden(hidden).build();
	}

	protected void setProvider(String name, RemoteServiceContainerInstantiator inst, boolean server) {
		setProvider(name, inst, server, false);
	}

	public IRemoteServiceDistributionProvider getProvider() {
		return this.provider;
	}

	public DirectRemoteServiceDistributionProvider(String name, RemoteServiceContainerInstantiator inst, boolean server,
			boolean hidden) {
		setProvider(name, inst, server, hidden);
	}

	public DirectRemoteServiceDistributionProvider(String name, RemoteServiceContainerInstantiator inst,
			boolean server) {
		setProvider(name, inst, server);
	}
}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.util;

import java.util.Map;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceContainerInstantiator;

/**
 * Distribution provider implmentation for direct remote service clients.
 * 
 * @author slewis
 *
 */
public class DirectRemoteServiceClientDistributionProvider extends DirectRemoteServiceDistributionProvider {

	public DirectRemoteServiceClientDistributionProvider(String clientProvider, String hostProvider,
			final IDirectContainerInstantiator inst, String[] supportedIntents) {
		super(clientProvider, new RemoteServiceContainerInstantiator(hostProvider, clientProvider) {

			@Override
			public IContainer createInstance(ContainerTypeDescription description, Map<String, ?> parameters)
					throws ContainerCreateException {
				return inst.createContainer();
			}

			@Override
			public String[] getSupportedIntents(ContainerTypeDescription description) {
				return supportedIntents;
			}
		}, false);
	}

}

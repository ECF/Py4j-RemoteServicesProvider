/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.util;

import java.util.Map;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.direct.InternalServiceProvider;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;

/**
 * Implementation of DirectHostContainer for ECF remote service distribution providers.
 * 
 * @author slewis
 *
 */
public class DirectHostContainer extends AbstractRSAContainer {

	private InternalServiceProvider internalServiceProvider;

	public DirectHostContainer(ID id, InternalServiceProvider isp) {
		super(id);
		this.internalServiceProvider = isp;
	}

	@Override
	protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration) {
		this.internalServiceProvider.externalExport(registration.getID().getContainerRelativeID(),
				registration.getService());
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
		this.internalServiceProvider = null;
	}

	@Override
	protected void unexportRemoteService(RSARemoteServiceRegistration registration) {
	}

}

/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.local;

import java.util.Map;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.remoteservice.AbstractRSAContainer;
import org.eclipse.ecf.remoteservice.RSARemoteServiceContainerAdapter.RSARemoteServiceRegistration;

public class RSAHostContainer extends AbstractRSAContainer {

	private ContainerExporterService exporter;

	public RSAHostContainer(ID id, ContainerExporterService exporter) {
		super(id);
		this.exporter = exporter;
	}

	@Override
	protected Map<String, Object> exportRemoteService(RSARemoteServiceRegistration registration) {
		this.exporter.exportFromContainer(registration.getID().getContainerRelativeID(), registration.getService());
		return null;
	}

	@Override
	protected void unexportRemoteService(RSARemoteServiceRegistration registration) {
		// Do nothing
	}

	@Override
	public void dispose() {
		super.dispose();
		this.exporter = null;
	}

}

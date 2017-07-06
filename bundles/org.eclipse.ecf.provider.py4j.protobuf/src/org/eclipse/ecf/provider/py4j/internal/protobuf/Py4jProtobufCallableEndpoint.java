/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.internal.protobuf;

import org.eclipse.ecf.provider.direct.ExternalCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.AbstractProtobufCallableEndpoint;
import org.eclipse.ecf.provider.direct.protobuf.ProtobufCallableEndpoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
public class Py4jProtobufCallableEndpoint extends AbstractProtobufCallableEndpoint implements ProtobufCallableEndpoint {

	private static Py4jProtobufCallableEndpoint instance;

	public static Py4jProtobufCallableEndpoint getInstance() {
		return instance;
	}

	public ExternalCallableEndpoint getExternalCallableEndpoint() {
		return super.getExternalCallableEndpoint();
	}

	@Reference(target = "(directTargetPort=" + py4j.GatewayServer.DEFAULT_PYTHON_PORT + ")")
	protected void bindExternalCallableEndpoint(ExternalCallableEndpoint callable) {
		super.bindExternalCallableEndpoint(callable);
	}

	protected void unbindExternalCallableEndpoint(ExternalCallableEndpoint callable) {
		super.unbindExternalCallableEndpoint(callable);
	}

	@Activate
	void activate() {
		instance = this;
	}

	@Deactivate
	void deactivate() {
		instance = null;
	}
}

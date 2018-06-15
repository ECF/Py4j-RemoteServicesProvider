/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.protobuf.hello.provider;

import org.eclipse.ecf.python.PythonLaunchCommandProvider;
import org.eclipse.ecf.python.protobuf.ProtobufPythonLaunchCommandProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

//@Component(immediate=true)
public class ExampleProtobufPythonLaunchCommandProvider extends ProtobufPythonLaunchCommandProvider
		implements PythonLaunchCommandProvider {

	@Override
	@Activate
	protected void activate(BundleContext ctx) throws Exception {
		super.activate(ctx);
	}
	
	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}
}

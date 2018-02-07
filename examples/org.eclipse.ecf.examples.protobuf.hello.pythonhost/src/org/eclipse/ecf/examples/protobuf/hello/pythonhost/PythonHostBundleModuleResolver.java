/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.protobuf.hello.pythonhost;

import java.util.Map;

import org.eclipse.ecf.provider.direct.BundleModuleResolver;
import org.eclipse.ecf.provider.direct.ModuleResolver;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(immediate=true,property={ BundleModuleResolver.PATH_PREFIX_PROP+"="+"/python-src" })
public class PythonHostBundleModuleResolver extends BundleModuleResolver implements ModuleResolver {

	@Activate
	protected void activate(BundleContext context, @SuppressWarnings("rawtypes") Map properties) {
		super.activate(context, properties);
	}
	
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
}

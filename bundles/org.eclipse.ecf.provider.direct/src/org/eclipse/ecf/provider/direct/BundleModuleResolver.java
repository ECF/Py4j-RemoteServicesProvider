/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

public interface BundleModuleResolver {

	public static final String PATH_PREFIX_PROP = "pathPrefix";
	public static final String PACKAGE_SUFFIX = "/";
	public static final String PACKAGE_INIT_NAME = "__init__";
	public static final String MODULE_SUFFIX = ".py";
	public static final String PACKAGE_INIT_FILE = PACKAGE_SUFFIX + PACKAGE_INIT_NAME + MODULE_SUFFIX;
	int getModuleType(String modulename);
	String getModuleCode(String modulename, boolean ispackage) throws Exception;
	
}

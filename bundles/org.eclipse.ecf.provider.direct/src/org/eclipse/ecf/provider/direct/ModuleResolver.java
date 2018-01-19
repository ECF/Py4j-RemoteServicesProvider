/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

public interface ModuleResolver {
	public static final int NONE = 0;
	public static final int PACKAGE = 1;
	public static final int MODULE = 2;
	
	int getModuleType(String moduleName);
	String getModuleCode(String moduleName, boolean ispackage) throws Exception;
	
}

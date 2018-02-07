/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python;

public interface PythonLaunchCommandProvider {

	public static final String DEFAULT_MAIN_PATH = System.getProperty("org.eclipse.ecf.python.defaultMainPath",
			"python-src/__main__.py");

	String getLaunchCommand();
}

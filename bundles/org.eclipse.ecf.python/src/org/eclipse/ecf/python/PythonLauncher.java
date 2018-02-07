/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python;

import java.io.OutputStream;

public interface PythonLauncher {

	public static final String JAVA_PORT_OPTION = "-j";
	public static final String PYTHON_PORT_OPTION = "-p";

	boolean isLaunched();

	void launch(String[] args, OutputStream output) throws Exception;

	void halt();

}

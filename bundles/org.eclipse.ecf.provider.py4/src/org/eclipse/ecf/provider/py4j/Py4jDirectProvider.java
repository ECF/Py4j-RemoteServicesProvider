/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j;

import org.eclipse.ecf.provider.direct.DirectProvider;

public interface Py4jDirectProvider extends DirectProvider {

	int getPythonPort();
	String getPythonAddress();
	int getJavaPort();
	String getJavaAddress();

}

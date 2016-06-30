/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.util.Map;

public interface DirectRemoteServiceProvider {

	public static final String EXTERNAL_SERVICE_PROP = "org.eclipse.ecf.provider.direct.DirectRemoteServiceProvider.external";

	@SuppressWarnings("rawtypes")
	void exportService(Object service, Map rsaProps);

	@SuppressWarnings("rawtypes")
	void updateService(Map rsaProps);

	@SuppressWarnings("rawtypes")
	void unexportService(Map rsaProps);

}

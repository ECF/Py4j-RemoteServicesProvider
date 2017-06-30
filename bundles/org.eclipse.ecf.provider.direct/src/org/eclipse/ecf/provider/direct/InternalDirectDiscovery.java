/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

import java.util.Map;

public interface InternalDirectDiscovery {

	public static final String DIRECT_TARGET_ID_PROP = "directTargetId";
	public static final String DIRECT_TARGET_PORT_PROP = "directTargetPort";

	void _external_discoverService(Object service, @SuppressWarnings("rawtypes") Map rsaMap);

	void _external_updateDiscoveredService(@SuppressWarnings("rawtypes") Map rsaMap);

	void _external_undiscoverService(@SuppressWarnings("rawtypes") Map rsaMap);
}

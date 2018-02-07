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

/**
 * A direct discovery service that allows Java-exported services to call out to allow the external process to discover
 * Java-implemented services. For example, when these methods are called as part of the RSA export, the connected Python
 * process will be notified, allowing it to discover java-exported services.
 * 
 * @author slewis
 *
 */
public interface InternalDirectDiscovery {

	void _external_discoverService(Object service, @SuppressWarnings("rawtypes") Map rsaMap);

	void _external_updateDiscoveredService(@SuppressWarnings("rawtypes") Map rsaMap);

	void _external_undiscoverService(@SuppressWarnings("rawtypes") Map rsaMap);
}

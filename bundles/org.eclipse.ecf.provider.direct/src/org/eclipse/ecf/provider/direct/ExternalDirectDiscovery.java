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
 * Interface exposed to external processes for directly discovering services
 * exported by the external process. For example, services hosted/implemented in
 * Python are made available as java remote services by using this interface.
 * 
 * @author slewis
 *
 */
public interface ExternalDirectDiscovery {

	@SuppressWarnings("rawtypes")
	void _java_discoverService(Object service, Map rsaProps);

	@SuppressWarnings("rawtypes")
	void _java_updateDiscoveredService(Map rsaProps);

	@SuppressWarnings("rawtypes")
	void _java_undiscoverService(Map rsaProps);

}

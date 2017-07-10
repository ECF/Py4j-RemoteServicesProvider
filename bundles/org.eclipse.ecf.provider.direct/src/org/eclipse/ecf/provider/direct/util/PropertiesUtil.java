/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.ecf.provider.direct.ExternalServiceProvider;

/**
 * Properties utils support class.
 * 
 * @author slewis
 *
 */
public class PropertiesUtil {

	public static Long convertPropToLong(@SuppressWarnings("rawtypes") Map input, String key) {
		Object val = input.get(key);
		if (val instanceof Long)
			return (Long) val;
		else if (val instanceof Integer)
			return ((Integer) val).longValue();
		else if (val instanceof String)
			return new Long((String) val);
		else
			return new Long(0);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map conditionProperties(Map input, String proxyId) {
		if (input == null)
			throw new NullPointerException("input properties cannot be null");
		Map result = new TreeMap();
		for (Object key : input.keySet())
			result.put(key, input.get(key));

		Object oc = result.get(org.osgi.framework.Constants.OBJECTCLASS);
		if (oc instanceof List) {
			oc = ((List) oc).toArray(new String[] {});
			result.put(org.osgi.framework.Constants.OBJECTCLASS, oc);
		}
		result.put("endpoint.service.id", convertPropToLong(result, "endpoint.service.id"));
		result.put(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID,
				convertPropToLong(result, org.eclipse.ecf.remoteservice.Constants.SERVICE_ID));
		result.put("service.ranking", convertPropToLong(result, "service.ranking"));
		result.put(ExternalServiceProvider.PROXYID_PROP_NAME, proxyId);
		return result;
	}

}

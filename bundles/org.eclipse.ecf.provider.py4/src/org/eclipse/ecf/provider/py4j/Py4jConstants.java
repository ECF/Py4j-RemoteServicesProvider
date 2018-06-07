/*******************************************************************************
 * Copyright (c) 2016 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j;

/**
 * Py4j-specific provider constants.
 * 
 * @author slewis
 *
 */
public interface Py4jConstants {

	public static final String NAMESPACE_NAME = "ecf.namespace.py4j";
	public static final String NAMESPACE_PROTOCOL = "py4j";
	public static final String NAMESPACE_JAVA_PATH = "/java";
	public static final String NAMESPACE_PYTHON_PATH = "/python";

	public static final String JAVA_HOST_CONFIG_TYPE = "ecf.py4j.host";
	public static final String JAVA_CONSUMER_CONFIG_TYPE = "ecf.py4j.consumer";
	public static final String PYTHON_HOST_CONFIG_TYPE = "ecf.py4j.host.python";
	public static final String PYTHON_CONSUMER_CONFIG_TYPE = "ecf.py4j.consumer.python";
}

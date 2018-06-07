/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.py4j.protobuf;

/**
 * Constants (remote service distribution provider ids)
 * 
 * @author slewis
 *
 */
public interface ProtobufPy4jConstants {

	public static final String JAVA_HOST_CONFIG_TYPE = "ecf.py4j.host.pb";
	public static final String PYTHON_HOST_CONFIG_TYPE = "ecf.py4j.host.python.pb";
	public static final String JAVA_CONSUMER_CONFIG_TYPE = "ecf.py4j.consumer.pb";
	public static final String PYTHON_CONSUMER_CONFIG_TYPE = "ecf.py4j.consumer.python.pb";

	public static final String JAVA_PROTOBUF_HOST_CONFIG_TYPE = "ecf.py4j.protobuf.host";
	public static final String JAVA_PROTOBUF_CONSUMER_CONFIG_TYPE = "ecf.py4j.protobuf.consumer";
	public static final String PYTHON_PROTOBUF_HOST_CONFIG_TYPE = "ecf.py4j.python.protobuf.host";
	public static final String PYTHON_PROTOBUF_CONSUMER_CONFIG_TYPE = "ecf.py4j.python.protobuf.consumer";

}

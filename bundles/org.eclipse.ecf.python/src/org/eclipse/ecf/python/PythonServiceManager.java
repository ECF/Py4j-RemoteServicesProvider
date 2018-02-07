/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PythonServiceManager {

	CompletableFuture<String> createAndExport(String module, String className, Map<String, String> args,
			Map<String, String> exportProperties);

	CompletableFuture<Void> unexport(String endpointId);

	Collection<String> getEndpointIds();

}

/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.provider.direct;

/**
 * Java-side internal service provider. This allows RSA remote service host containers (e.g. DirectHostContainer to
 * export remote services.
 * 
 * @author slewis
 *
 */
public interface InternalServiceProvider {

	void externalExport(long rsId, Object service);

}

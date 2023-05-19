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
 * An external endpoint that is callable via a long remote service id (rsId), a
 * String methodName, and a byte[] representing serialized arguments
 * (serializedArgs) This interface may be used by provider that serialize their
 * args in a custom way (e.g. protocol buffers).
 * 
 * @author slewis
 */
public interface ExternalCallableEndpoint {

	byte[] _call_endpoint(Long rsId, String methodName, byte[] serializedArgs) throws Exception;

}

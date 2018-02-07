/*******************************************************************************
 * Copyright (c) 2017 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python.protobuf;

import org.eclipse.ecf.python.protobuf.Exporter.ExportRequest;
import org.eclipse.ecf.python.protobuf.Exporter.ExportResponse;
import org.eclipse.ecf.python.protobuf.Exporter.UnexportRequest;
import org.eclipse.ecf.python.protobuf.Exporter.UnexportResponse;

public interface PythonServiceExporter {

	ExportResponse createAndExport(ExportRequest request);

	UnexportResponse unexport(UnexportRequest request);
}

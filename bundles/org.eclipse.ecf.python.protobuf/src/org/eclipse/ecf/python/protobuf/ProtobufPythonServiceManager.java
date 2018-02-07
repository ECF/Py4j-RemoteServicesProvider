/*******************************************************************************
 * Copyright (c) 2018 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.python.protobuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.ecf.python.PythonServiceManager;
import org.eclipse.ecf.python.protobuf.Exporter.ExportRequest;
import org.eclipse.ecf.python.protobuf.Exporter.UnexportRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class ProtobufPythonServiceManager implements PythonServiceManager {

	private static Logger logger = LoggerFactory.getLogger(ProtobufPythonServiceManager.class);

	private PythonServiceExporterAsync exporter;
	private List<String> endpointIds = Collections.synchronizedList(new ArrayList<String>());

	@Reference
	void bindPythonServiceExporterAsync(PythonServiceExporterAsync exporter) {
		this.exporter = exporter;
	}

	void unbindPythonServiceExporter(PythonServiceExporter exporter) {
		this.exporter = null;
	}

	@Override
	public CompletableFuture<String> createAndExport(String module, String className, Map<String, String> args,
			Map<String, String> exportProperties) {
		ExportRequest.Builder requestBuilder = ExportRequest.newBuilder();
		requestBuilder.setModuleName(module);
		requestBuilder.setClassName(className);
		if (args != null)
			for (String key : args.keySet())
				requestBuilder.putCreationArgs(key, args.get(key));
		if (exportProperties != null)
			for (String key : exportProperties.keySet())
				requestBuilder.putOverridingExportProps(key, exportProperties.get(key));
		ExportRequest request = requestBuilder.build();
		logger.debug("createAndExport module=" + module + ";className=" + className);
		final CompletableFuture<String> futureResult = new CompletableFuture<String>();
		this.exporter.createAndExportAsync(request).whenComplete((resp, t) -> {
			if (t != null) {
				logger.debug("Exception in createAndExportAsync", t);
				futureResult.completeExceptionally(t);
			} else {
				String endpointId = resp.getEndpointId();
				if (endpointId == null || "".equals(endpointId)) {
					logger.debug("createAndExport failed with message=" + resp.getErrorMessage());
					futureResult.completeExceptionally(
							new NullPointerException("Python service create exception: " + resp.getErrorMessage()));
				} else {
					logger.debug("createAndExport module=" + module + ";classname=" + className + " exportedId="
							+ endpointId);
					endpointIds.add(endpointId);
					futureResult.complete(endpointId);
				}
			}
		});
		return futureResult;
	}

	@Override
	public CompletableFuture<Void> unexport(String endpointId) {
		UnexportRequest.Builder requestBuilder = UnexportRequest.newBuilder();
		requestBuilder.setEndpointId(endpointId);
		CompletableFuture<Void> result = new CompletableFuture<Void>();
		this.exporter.unexportAsync(requestBuilder.build()).whenComplete((resp, t) -> {
			if (t != null) {
				logger.debug("Exception in unexport", t);
				result.completeExceptionally(t);
			} else {
				boolean success = resp.getSuccess();
				if (!success) {
					logger.debug("unexport failed with message=" + resp.getMessage());
					result.completeExceptionally(
							new NullPointerException("Python service unexport exception: " + resp.getMessage()));
				} else {
					this.endpointIds.remove(endpointId);
					logger.debug("unexport succeeded with endpointId=" + endpointId);
				}
			}
		});
		return result;
	}

	@Override
	public Collection<String> getEndpointIds() {
		return new ArrayList<String>(this.endpointIds);
	}

}

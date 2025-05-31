/*******************************************************************************
 * Copyright (c) 2025 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.examples.ai.mcp.toolservice.api;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolAnnotations;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;
import org.eclipse.ecf.ai.mcp.tools.service.ToolGroupService;

/**
 * This interface describes a Model Context Protocol 'tool group'.  A tool group
 * is 1 or more methods, declared by this interface (and all extended interfaces)
 * with the Tool annotation, and optionally ToolAnnotations, ToolResult, and ToolParam annotations.
 * The meta-data (descriptions, titles, etc) can/will then be used by a MCP server
 * to make available the use of these tools to MCP clients (e.g. LLMs, other machine
 * learning systems).
 * 
 * This is a simple example that exposes 'add' and 'multiply' tools as a single ArithmeticTools
 * service/remote service.
 */
public interface ArithmeticTools extends ToolGroupService {

	@Tool(description = "computes the sum of the two integer arguments")
	@ToolAnnotations(destructiveHint = true, title="howdy")
	@ToolResult(description = "the integer result for this tool")
	int add(@ToolParam(description = "a is the first argument") int a, @ToolParam(description = "b is the second argument") int b);

	@Tool(description = "return the product of the two given integer arguments named a and b")
	int multiply(@ToolParam(description = "a is the first argument") int a, @ToolParam(description = " b is the second argument") int b);

}

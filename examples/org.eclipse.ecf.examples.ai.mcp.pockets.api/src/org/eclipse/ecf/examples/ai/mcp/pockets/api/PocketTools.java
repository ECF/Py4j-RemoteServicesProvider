package org.eclipse.ecf.examples.ai.mcp.pockets.api;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;
import org.eclipse.ecf.ai.mcp.tools.service.ToolGroupService;

public interface PocketTools extends ToolGroupService {

	@Tool(description="tool to create new pockets given a PocketSpec")
	@ToolResult(description = "returns the new pocket's id if successfully created.  Null if unsuccessful")
	String createPocket(@ToolParam(description="the specification for the new pocket") PocketSpec spec);
	
	boolean removePocket(String pocketId, String dateTimeSpec, String memo);
	
	
}

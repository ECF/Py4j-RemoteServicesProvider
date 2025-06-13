package org.eclipse.ecf.examples.ai.mcp.pockets.api;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;
import org.eclipse.ecf.ai.mcp.tools.service.ToolGroupService;

public interface ProfileTools extends ToolGroupService {
	
	@Tool(description="create a new user profile for the pockets service")
	@ToolResult(description = "returns true if the profile was created, false if not")
	boolean createProfile(@ToolParam(description = "the new profile name or id") String nameOrId, 
			@ToolParam(description = "the new profile description") String description,
			@ToolParam(description = "the new profile preset.  Either 'dnd5e' 'pf2e' or null (default=dnd5e)") String preset);
	
	@Tool(description="export the user profile identified by nameOrId parameter to json format")
	@ToolResult(description = "json formatted user profile")
	String exportProfile(@ToolParam(description = "previously given name or id for the profile to export") String nameOrId);
	
	@Tool(description="import a user profile from the given json")
	@ToolResult(description = "returns true if the profile was successfully imported, false otherwise (because of nameOrId collision)")
	boolean importProfile(@ToolParam(description = "json formatted profile to import") String json);
	
	@Tool(description="remove a user profile given the nameOrId from first input parameter")
	@ToolResult(description = "json formatted user profile removed")
	String removeProfile(@ToolParam(description = "previously given name or id for the profile to remove") String nameOrId);
	
}

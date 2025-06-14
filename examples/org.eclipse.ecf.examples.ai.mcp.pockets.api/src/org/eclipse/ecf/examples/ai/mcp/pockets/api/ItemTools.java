package org.eclipse.ecf.examples.ai.mcp.pockets.api;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;
import org.eclipse.ecf.ai.mcp.tools.service.ToolGroupService;

public interface ItemTools extends ToolGroupService {

	@Tool(description = "tool to create new item given an ItemSpec")
	@ToolResult(description = "returns the new item's id if successfully created.  Null if unsuccessful")
	String createItem(@ToolParam(description = "the specification for the new pocket") ItemSpec spec, String profileId);

	@Tool(description = "tool to remove an item identified by itemId")
	@ToolResult(description = "returns true if successfully removed.  False if unsuccessful")
	boolean removeItem(@ToolParam(description = "itemId is the unique id for the pocket to remove") String itemId,
			@ToolParam(description = "dateTime spec") String dateTimeSpec,
			@ToolParam(description = "a memo associated with the removal") String removeMemo);

}

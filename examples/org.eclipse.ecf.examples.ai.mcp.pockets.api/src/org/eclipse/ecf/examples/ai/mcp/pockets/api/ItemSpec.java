package org.eclipse.ecf.examples.ai.mcp.pockets.api;

import java.util.Map;

public record ItemSpec(String name, String dateTime, String memo, String type, String notes,
		Map<String, String> attributes) {

}

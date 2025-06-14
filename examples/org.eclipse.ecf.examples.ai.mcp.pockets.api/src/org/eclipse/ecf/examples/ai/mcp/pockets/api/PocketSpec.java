package org.eclipse.ecf.examples.ai.mcp.pockets.api;

import java.util.Map;

public record PocketSpec(String name, String type, String from, Map<String, String> attributes) {

}

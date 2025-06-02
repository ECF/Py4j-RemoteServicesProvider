package org.eclipse.ecf.example_mcp_client;

import java.time.Duration;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArithmeticToolsMCPClient {

	private static final Logger logger = LoggerFactory.getLogger(ArithmeticToolsMCPClient.class);
	
	public static void main(String[] args) {
		// This app requires two args to start the Python Arithmetic Server:
		// 1.  Full path to python exe ...e.g. C:\\Python313\\python.exe
		// 2.  Full path to run_framework.py...e.g. C:\\path to ipopo git repo clone\\ipopo\\samples\\remotetoolsserver\\run_framework.py
		StdioClientTransport transport = new StdioClientTransport(ServerParameters.builder(args[0])
			    .args(args[1])
			    .build());
		McpSyncClient client = McpClient.sync(transport)
		    .requestTimeout(Duration.ofSeconds(1000))
		    .capabilities(ClientCapabilities.builder()
		        .build())
	        .loggingConsumer(notification -> {
	            System.out.println("Received log message: " + notification.data());
	        })
		    .build();

		// Initialize connection
		client.initialize();
		
		// List available tools
		ListToolsResult tools = client.listTools();
		logger.debug("tools=" + tools);

		// Call 'add' with values 2, 3
		callTool(client, "add", Map.of("a", 2, "b", 3));

		// Call 'multiply' with values 500 and 23
		callTool(client, "multiply", Map.of("a", 500, "b", 23));

		// Close client
		client.closeGracefully();	
	}

	private static void callTool(McpSyncClient client, String tool_name, Map<String,Object> args) {
		logger.debug("Calling tool:  "+ tool_name + "(" + args + ")");
		CallToolResult result = client.callTool(
			    new CallToolRequest(tool_name, 
			        args)
			);
		logger.debug("result=" + ((TextContent) result.content().get(0)).text());
	}
}

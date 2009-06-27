package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.json.JSONException;
import org.json.XML;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.felixbruns.jotify.gateway.util.URIUtilities;

public abstract class GatewayHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		/* Get request method and query. */
		String requestMethod = exchange.getRequestMethod();
		String requestQuery  = exchange.getRequestURI().getQuery();
		
		/* Get request parameters. */
		Map<String, String> params = URIUtilities.parseQuery(requestQuery);
		
		/* Get response body and headers. */
		OutputStream responseBody    = exchange.getResponseBody();
		Headers      responseHeaders = exchange.getResponseHeaders();
		String       responseString;
		byte[]       responseBytes;
		
		/* Set Access-Control headers.*/
		responseHeaders.set("Access-Control-Allow-Origin", "*");
		responseHeaders.set("Access-Control-Allow-Methods", "GET, OPTIONS");
		responseHeaders.set("Access-Control-Allow-Headers", "X-Requested-With");
		responseHeaders.set("Access-Control-Max-Age", "1728000");
		
		/* Get response string depending on request method. */
		if(requestMethod.equalsIgnoreCase("OPTIONS")){
			responseString = "";
		}
		else if(requestMethod.equalsIgnoreCase("GET")){
			responseString = this.handle(params);
		}
		else{
			responseString = "<error>Method not supported</error>";
		}
		
		/* Set 'Content-Type' header and encode response depending on output format. */
		if(params.get("format") != null && params.get("format").equals("json")){
			responseHeaders.set("Content-Type", "application/json");
			
			/* Convert XML to JSON object or fail with JSON encoded error message. */
			try{
				responseString = XML.toJSONObject(responseString).toString(4);
			}
			catch(JSONException e){
				responseString = "{\"error\": \"" + e.getMessage().replace("\"", "\\\"") + "\"}";
			}
		}
		else{
			responseHeaders.set("Content-Type", "text/xml");
		}
		
		/* Get response bytes. */
		responseBytes = responseString.getBytes(Charset.forName("UTF-8"));
		
		/* Send response code, length and headers. */
		exchange.sendResponseHeaders(200, responseBytes.length);
		
		/* Write response string to output stream. */
		responseBody.write(responseBytes);
		responseBody.close();
	}
	
	public abstract String handle(Map<String, String> params);
}

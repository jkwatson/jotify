package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class JotifyGateway {
	public static Map<String, GatewaySession> sessions;
	public static ExecutorService             executor;
	
	/* Statically create session map and executor for sessions. */
	static {
		sessions = new HashMap<String, GatewaySession>();
		executor = Executors.newCachedThreadPool();
	}
	
	/* Main thread to listen for client connections. */
	public static void main(String[] args) throws IOException {
		/* Create a HTTP server that listens for connections on port 8080. */
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		
		/* Set up content handlers. */
		server.createContext("/",          new ContentHandler("resources/index.html", "text/html"));
		server.createContext("/style.css", new ContentHandler("resources/style.css",  "text/css"));
		server.createContext("/jquery.js", new ContentHandler("resources/jquery.js",  "text/javascript"));
		server.createContext("/icon.png",  new ContentHandler("resources/icon.png",   "image/png"));
		server.createContext("/logo.png",  new ContentHandler("resources/logo.png",   "image/png"));
		
		/* Set up gateway handlers. */
		server.createContext("/auth",      new AuthenticationHandler());
		server.createContext("/close",     new CloseHandler());
		server.createContext("/search",    new SearchHandler());
		server.createContext("/image",     new ImageHandler());
		server.createContext("/browse",    new BrowseHandler());
		server.createContext("/playlist",  new PlaylistHandler());
		server.createContext("/stream",    new StreamHandler());
		
		/* Set executor for server threads. */
		server.setExecutor(Executors.newCachedThreadPool());
				
		/* Start HTTP server. */
		server.start();
	}
}

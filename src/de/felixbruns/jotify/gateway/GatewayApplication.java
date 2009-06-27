package de.felixbruns.jotify.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

import de.felixbruns.jotify.gateway.handlers.*;

public class GatewayApplication {
	public static Map<String, GatewayConnection> sessions;
	public static ExecutorService                executor;
	
	/* Statically create session map and executor for sessions. */
	static {
		sessions = new HashMap<String, GatewayConnection>();
		executor = Executors.newCachedThreadPool();
	}
	
	/* Main thread to listen for client connections. */
	public static void main(String[] args) throws IOException {
		/* Create a HTTP server that listens for connections on port 8080. */
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		
		/* Set up content handlers. */
		server.createContext("/",       new ContentHandler());
		server.createContext("/images", new ContentHandler());
		
		/* Set up gateway handlers. */
		server.createContext("/start",    new StartHandler());
		server.createContext("/check",    new CheckHandler());
		server.createContext("/login",    new LoginHandler());
		server.createContext("/close",    new CloseHandler());
		server.createContext("/user",     new UserHandler());
		server.createContext("/toplist",  new ToplistHandler());
		server.createContext("/search",   new SearchHandler());
		server.createContext("/image",    new ImageHandler());
		server.createContext("/browse",   new BrowseHandler());
		server.createContext("/playlist", new PlaylistHandler());
		server.createContext("/stream",   new StreamHandler());
		
		/* Set executor for server threads. */
		server.setExecutor(executor);
		
		/* Start HTTP server. */
		server.start();
	}
}

package de.felixbruns.jotify.util;

import java.util.ArrayList;
import java.util.Collection;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class ServerLookup {
	private static ServerLookup instance;
	
	static{
		instance = new ServerLookup();
	}
	
	public static Collection<Server> lookupServers(String dnsName){
		return lookupServers(dnsName, new ArrayList<Server>());
	}
	
	public static Collection<Server> lookupServers(String dnsName, Server fallback){
		Collection<Server> fallbackList = new ArrayList<Server>();
		
		fallbackList.add(fallback);
		
		return lookupServers(dnsName, fallbackList);
	}
	
	public static Collection<Server> lookupServers(String dnsName, Collection<Server> fallbackList){
		Collection<Server> servers = new ArrayList<Server>();
		Lookup             lookup  = null;
		Record[]           records = null;;
		
		try{
			lookup = new Lookup(dnsName, Type.SRV);
		}
		catch(TextParseException e){
			System.err.println("Error parsing DNS name: " + e.getMessage());
		}
		
		if(lookup == null || (records = lookup.run()) == null){
			if(fallbackList != null){
				servers.addAll(fallbackList);
			}
			
			return servers;
		}
		
		for(Record record : records){
			if(record instanceof SRVRecord){
				SRVRecord srvRecord = (SRVRecord)record;
				
				servers.add(instance.new Server(
					srvRecord.getAdditionalName().toString(), srvRecord.getPort()
				));
			}
		}
		
		return servers;
	}
	
	public static Server createServer(String hostname, int port){
		return instance.new Server(hostname, port);
	}
	
	public class Server{
		private String hostname;
		private int    port;
		
		public Server(String hostname, int port){
			this.hostname = hostname;
			this.port     = port;
		}

		public String getHostname(){
			return this.hostname;
		}

		public int getPort(){
			return this.port;
		}
		
		public String toString(){
			return this.hostname + ":" + this.port;
		}
	}
}

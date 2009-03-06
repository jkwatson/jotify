package de.felixbruns.spotify.util;

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
		Collection<Server> servers = new ArrayList<Server>();
		Lookup             lookup  = null;
		
		try{
			lookup = new Lookup(dnsName, Type.SRV);
		}
		catch(TextParseException e){
			System.err.println("Error parsing DNS name: " + e.getMessage());
		}
		
		if(lookup == null){
			return null;
		}
		
		for(Record record : lookup.run()){
			if(record instanceof SRVRecord){
				SRVRecord srvRecord = (SRVRecord)record;
				
				servers.add(instance.new Server(
					srvRecord.getAdditionalName().toString(), srvRecord.getPort()
				));
			}
		}
		
		return servers;
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

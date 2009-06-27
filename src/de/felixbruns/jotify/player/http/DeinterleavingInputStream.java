package de.felixbruns.jotify.player.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import de.felixbruns.jotify.util.Hex;

public class DeinterleavingInputStream extends InputStream {
	private InputStream[] streams;
	private int           stream;
	
	public DeinterleavingInputStream(Collection<InputStream> streams){
		if(streams == null || streams.size() < 2){
			throw new IllegalArgumentException("You need to supply at least 2 streams!");
		}
		
		this.streams = streams.toArray(new InputStream[0]);
		this.stream  = 0;
	}
	
	public DeinterleavingInputStream(InputStream... streams){
		if(streams == null || streams.length < 2){
			throw new IllegalArgumentException("You need to supply at least 2 streams!");
		}
		
		this.streams = streams;
		this.stream  = 0;
	}
	
	public int read() throws IOException {
		int value = this.streams[this.stream].read();
		
		if(value == -1 && this.stream != 0){
			throw new IOException("Unexpected end of stream " + (this.stream + 1) + "!");			
		}
		
		this.stream = (this.stream + 1) % this.streams.length;
		
		return value;
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		int read = 0;
		int value;
		
		for(int i = off; i < off + len; i++, read++){
			if((value = this.read()) == -1){
				break;
			}
			
			b[i] = (byte)value;
		}
		
		return read;
	}
	
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	public static void main(String[] args) throws Exception {
		byte[] a = new byte[10];
		byte[] b = new byte[10];
		byte[] c = new byte[10];
		byte[] d = new byte[10];
		
		Arrays.fill(a, (byte)0xAA);
		Arrays.fill(b, (byte)0xBB);
		Arrays.fill(c, (byte)0xCC);
		Arrays.fill(d, (byte)0xDD);
		
		InputStream as = new ByteArrayInputStream(a);
		InputStream bs = new ByteArrayInputStream(b);
		InputStream cs = new ByteArrayInputStream(c);
		InputStream ds = new ByteArrayInputStream(d);
		
		InputStream s = new DeinterleavingInputStream(as, bs, cs, ds);
		
		/*int value;
		
		while((value = s.read()) != -1){
			System.out.format("%02x", value);
		}*/
		
		byte[] buf = new byte[40];
		
		System.out.println(s.read(buf));
		System.out.println(Hex.toHex(buf));
	}
}

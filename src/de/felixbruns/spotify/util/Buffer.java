package de.felixbruns.spotify.util;

import java.util.Arrays;

public class Buffer {
	private byte[] buffer;
	private int    size;
	private int    position;
	
	public Buffer(){
		this(256);
	}
	
	public Buffer(int initialCapacity){
		this.size     = initialCapacity;
		this.buffer   = new byte[this.size];
		this.position = 0;
	}
	
	public Buffer(byte[] buffer){
		this.buffer   = buffer;
		this.size     = buffer.length;
		this.position = buffer.length;
	}
	
	public int size(){
		return this.size;
	}
	
	public int position(){
		return this.position;
	}
	
	public void clear(){
		this.position = 0;
	}
	
	public void setByte(int position, byte b){
		if(position + 1 > this.size){
			throw new IllegalArgumentException();
		}
		
		this.buffer[position] = b;
	}
	
	public void setBytes(int position, byte[] buffer){
		this.setBytes(position, buffer, buffer.length);
	}
	
	public void setBytes(int position, byte[] buffer, int n){
		if(position + n > this.size){
			throw new IllegalArgumentException();
		}
		
		for(int i = 0; i < n; i++){
			this.buffer[position + i] = buffer[i];			
		}
	}
	
	public void setShort(int position, short s){
		if(position + 2 > this.size){
			throw new IllegalArgumentException();
		}
		
		this.buffer[position    ] = (byte)((s & 0xFF00) >> 8);
		this.buffer[position + 1] = (byte) (s & 0x00FF);
	}
	
	public void setInt(int position, int i){
		if(position + 4 > this.size){
			throw new IllegalArgumentException();
		}
		
		this.buffer[position    ] = (byte)((i & 0xFF000000) >> 24);
		this.buffer[position + 1] = (byte)((i & 0x00FF0000) >> 16);
		this.buffer[position + 2] = (byte)((i & 0x0000FF00) >> 8);
		this.buffer[position + 3] = (byte) (i & 0x000000FF);
	}
	
	public void setLong(int position, long l){
		if(position + 8 > this.size){
			throw new IllegalArgumentException();
		}
		
		this.buffer[position    ] = (byte)((l & 0xFF00000000000000L) >> 56);
		this.buffer[position + 1] = (byte)((l & 0x00FF000000000000L) >> 48);
		this.buffer[position + 2] = (byte)((l & 0x0000FF0000000000L) >> 40);
		this.buffer[position + 3] = (byte)((l & 0x000000FF00000000L) >> 32);	
		this.buffer[position + 4] = (byte)((l & 0x00000000FF000000L) >> 24);
		this.buffer[position + 5] = (byte)((l & 0x0000000000FF0000L) >> 16);
		this.buffer[position + 6] = (byte)((l & 0x000000000000FF00L) >> 8);
		this.buffer[position + 7] = (byte) (l & 0x00000000000000FFL);
	}
	
	public void setString(int position, String s){
		if(position + s.length() > this.size){
			throw new IllegalArgumentException();
		}
		
		this.setBytes(position, s.getBytes());
	}
	
	public void appendByte(byte b){
		while(this.position + 1 >= this.size){
			this.grow();
		}
		
		this.setByte(this.position, b);
		
		this.position++;
	}
	
	public void appendBytes(byte[] buffer){
		this.appendBytes(buffer, buffer.length);
	}
	
	public void appendBytes(byte[] buffer, int n){
		while(this.position + n >= this.size){
			this.grow();
		}
		
		this.setBytes(this.position, buffer, n);
		
		this.position += n;
	}
	
	public void appendShort(short s){
		while(this.position + 2 >= this.size){
			this.grow();
		}
		
		this.setShort(this.position, s);
		
		this.position += 2;
	}
	
	public void appendInt(int i){
		while(this.position + 4 >= this.size){
			this.grow();
		}
		
		this.setInt(this.position, i);
		
		this.position += 4;
	}
	
	public void appendLong(long l){
		while(this.position + 8 >= this.size){
			this.grow();
		}
		
		this.setLong(this.position, l);
		
		this.position += 8;
	}
	
	public void appendString(String s){
		while(this.position + s.length() >= this.size){
			this.grow();
		}
		
		this.setString(this.position, s);
		
		this.position += s.length();
	}
	
	public byte[] getBytes(){
		return Arrays.copyOf(this.buffer, this.position);
	}
	
	public byte[] getBytes(int from, int to){
		return Arrays.copyOfRange(this.buffer, from, to);
	}
	
	private void grow(){
		this.size <<= 2;
		this.buffer = Arrays.copyOf(this.buffer, this.size);
	}
	
	public static Buffer wrap(byte[] buffer){
		return new Buffer(buffer);
	}
	
	public String toString(){
		String s = String.format(
			"%s (size: %d, position: %d):\n",
			this.getClass().getSimpleName(),
			this.size, this.position
		);
		
		for(int i = 1; i <= this.position; i++){
			s += String.format("0x%02x ", this.buffer[i - 1]);
			
			if(i % 8 == 0){
				s += "\n";
			}
		}
		
		return s;
	}
}

package de.felixbruns.jotify.media;

public class PlaylistConfirmation {
	private long    revision;
	private long    checksum;
	private boolean collaborative;
	
	public PlaylistConfirmation(){
		this.revision      = -1;
		this.checksum      = -1;
		this.collaborative = false;
	}
	
	public long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	public boolean isCollaborative(){
		return this.collaborative;
	}
	
	public void setCollaborative(boolean collaborative){
		this.collaborative = collaborative;
	}
	
	public long getChecksum(){
		return this.checksum;
	}
	
	public void setChecksum(long checksum){
		this.checksum = checksum;
	}
}

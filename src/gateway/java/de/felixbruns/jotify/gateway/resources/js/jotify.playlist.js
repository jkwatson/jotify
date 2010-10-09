/*
 *
 * Copyright (c) 2009, Felix Bruns <felixbruns@web.de>
 *
 */

function PlaylistContainer(data){
	var nextChange = data.playlist['next-change'];
	var ids        = nextChange.change.ops.add.items.split(',');
	
	this.author    = nextChange.change.user;
	this.time      = new Date(nextChange.change.time * 1000);
	this.revision  = parseInt(nextChange.version.split(',')[0], 10);
	this.playlists = [];
	
	for(index in ids){
		this.playlists.push(ids[index].substr(0, 32));
	}
}

/* Get author. */
PlaylistContainer.prototype.getAuthor = function(){
	return this.author;
};

/* Get time. */
PlaylistContainer.prototype.getTime = function(){
	return this.time;
};

/* Get revision. */
PlaylistContainer.prototype.getRevision = function(){
	return this.revision;
};

/* Get playlists. */
PlaylistContainer.prototype.getPlaylists = function(){
	return this.playlists;
};

function Playlist(data){
	var nextChange = data.playlist['next-change'];
	var ids        = nextChange.change.ops.add.items.split(',');
	
	this.author    = nextChange.change.user;
	this.time      = new Date(nextChange.change.time * 1000);
	this.revision  = parseInt(nextChange.version.split(',')[0], 10);
	this.name      = nextChange.change.ops.name;
	this.tracks    = [];
	
	for(index in ids){
		this.tracks.push(ids[index].substr(0, 32));
	}
}

/* Get author. */
Playlist.prototype.getAuthor = function(){
	return this.author;
};

/* Get time. */
Playlist.prototype.getTime = function(){
	return this.time;
};

/* Get revision. */
Playlist.prototype.getRevision = function(){
	return this.revision;
};

/* Get name. */
Playlist.prototype.getName = function(){
	return this.name;
};

/* Get tracks. */
Playlist.prototype.getTracks = function(){
	return this.tracks;
};

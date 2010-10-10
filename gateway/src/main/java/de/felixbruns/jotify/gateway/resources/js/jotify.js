/*
 *
 * Copyright (c) 2009, Felix Bruns <felixbruns@web.de>
 *
 */

function Jotify(options){
	/* Set default values for required options. */
	this.gateway = options.gateway || 'http://' + window.location.hostname + ':' + window.location.port;
}

/* Gateway request. */
Jotify.prototype.request = function(handler, params, callbacks, async){
	/* Set default values. */
	params    = params    || {};
	callbacks = callbacks || {};
	async     = (typeof(async) != 'undefined') ? async : true;
	
	/* Set format. */
	params.format = 'json';
	
	/* Request data. */
	$.ajax({
		data     : params,
		dataType : 'json',
		cache    : false,
		timeout  : 10000,
		type     : 'GET',
		async    : async,
		url      : this.gateway + '/' + handler,
		error    : function(xhr, textStatus, errorThrown){
			if(typeof(callbacks.error) != 'undefined'){
				callbacks.error('Error (' + textStatus + ')');
			}
		},
		success  : function(data, textStatus){
			/* Call user callback. */
			if(typeof(data.error) != 'undefined'){
				if(typeof(callbacks.error) != 'undefined'){
					callbacks.error(data.error);
				}
			}
			else if(typeof(callbacks.success) != 'undefined'){
				callbacks.success(data);
			}
		}
	});
};

/* Set gateway. */
Jotify.prototype.setGateway = function(_gateway){
	this.gateway = _gateway;
};

/* Start. */
Jotify.prototype.start = function(callbacks){
	this.request('start', {}, callbacks);
};

/* Check. */
Jotify.prototype.check = function(session, callbacks){
	this.request('check', {session: session}, callbacks);
};

/* Login. */
Jotify.prototype.login = function(params, callbacks){
	this.request('login', params, callbacks);
};

/* Close. */
Jotify.prototype.close = function(session, callbacks){
	this.request('close', {session: session}, callbacks);
};

/* User. */
Jotify.prototype.user = function(session, callbacks){
	this.request('user', {session: session}, callbacks);
};

/* Toplist. */
Jotify.prototype.toplist = function(params, session, callbacks){
	params.session = session;
	
	this.request('toplist', params, callbacks);
};

/* Search. */
Jotify.prototype.search = function(params, session, callbacks){
	params.session = session;
	
	this.request('search', params, callbacks);
};

/* Image. */
Jotify.prototype.image = function(id, session){
	return this.gateway + '/image?session=' + session + '&id=' + id;
};

/* Browse. */
Jotify.prototype.browse = function(params, session, callbacks, async){
	params.session = session;
	async          = (typeof(async) != 'undefined') ? async : true;
	
	this.request('browse', params, callbacks, async);
};

/* Playlists. */
Jotify.prototype.playlists = function(session, callbacks){
	this.request('playlists', {session : session}, callbacks);
};

/* Playlist. */
Jotify.prototype.playlist = function(params, session, callbacks, async){
	params.session = session;
	async          = (typeof(async) != 'undefined') ? async : true;
	
	this.request('playlist', params, callbacks, async);
};

/* Stream. */
Jotify.prototype.stream = function(track, session){
	var files = [];
	
	if(track.files.file instanceof Array){
		files = track.files.file;
	}
	else{
		files = [track.files.file];
	}
	
	return this.gateway + '/stream?session=' + session + '&id=' + track.id + '&file=' + files[0].id;
};

/* Play. */
Jotify.prototype.playTrack = function(track, session, callbacks){
	var files = [];
	
	if(track.files.file instanceof Array){
		files = track.files.file;
	}
	else{
		files = [track.files.file];
	}
	
	this.request('play', {
		session : session,
		id      : track.id,
		file    : files[0].id
	}, callbacks);
};

/* Pause. */
Jotify.prototype.play = function(session, callbacks){
	this.request('play', {session: session}, callbacks);
};

/* Pause. */
Jotify.prototype.pause = function(session, callbacks){
	this.request('pause', {session: session}, callbacks);
};

/* Stop. */
Jotify.prototype.stop = function(session, callbacks){
	this.request('stop', {session: session}, callbacks);
};

/*
 * Find identical tracks and group them together.
 *
 * Returns an array of tracks with each having an additional
 * 'identical-tracks' array.
 */
Jotify.prototype.groupIdenticalTracks = function(tracks){
	var groupedTracks = [];
	
	/* Outer loop over all tracks. */
	for(var i in tracks){
		/* Get track. */
		var track = tracks[i];
		
		/* Initialize 'identical-tracks' array. */
		track['identical-tracks'] = [];
		
		/* Inner loop over all tracks. */
		for(var j in tracks){
			/* If a track at a different position is identical. */
			if(i != j &&
				tracks[i].title  == tracks[j].title &&
				tracks[i].artist == tracks[j].artist){
				/* Add track to 'identical-tracks' array. */
				track['identical-tracks'].push(tracks[j]);
				
				/* Remove track from array. */
				delete tracks[j];
			}
		}
		
		/* Add track to new array. */
		groupedTracks.push(track);
		
		/* Remove track from array. */
		delete tracks[i];
	}
	
	/* Return grouped track array. */
	return groupedTracks;
};

/*
 * Sort tracks by given field.
 *
 * Returns a sorted array of tracks.
 */
Jotify.prototype.sortTracks = function(tracks, field, order){
	field = field || 'title';
	order = order || 'asc';
	
	var sortFunction = function(a, b){
		if(typeof a[field] == 'number'){
			return (order == 'asc') ? (a[field] - b[field]) : (b[field] - a[field]);
		}
		else if(typeof a[field] == 'string'){
			return (order == 'asc') ?
				((a[field] < b[field]) ? -1 : ((a[field] > b[field]) ? 1 : 0)) :
				((b[field] < a[field]) ? -1 : ((b[field] > a[field]) ? 1 : 0));
		}
		
		return 0;
	};
	
	return tracks.sort(sortFunction);
};



/* Draws a popularity indicator using canvas and returns it as CSS data URL. */
function popularityIndicator(popularity){
	/* Create a canvas. */
	var canvas = document.createElement('canvas');
	
	/* If drawing is not supported, return an empty CSS data URL. */
	if(!canvas.getContext){
		return '';
	}
	
	/* Get drawing context. */
	var ctx = canvas.getContext('2d');
	
	/* Set width and height. */
	canvas.width  = 100;
	canvas.height = 10;
	
	/* Calculate number of bars. */
	var bars   = (canvas.width - 10) / 3;
	var height = canvas.height - 2;
	
	/* Draw bars. */
	for(var i = 0, x = 5; i < bars; i++, x += 3){
		if(i < bars * popularity){
			ctx.fillStyle = 'rgb(0, 62, 136)';
			ctx.fillRect(x, canvas.height / 2 - height / 2 + 1, 2, height);
		}
		else{
			ctx.fillStyle = 'rgb(192, 192, 192)';
			ctx.fillRect(x, canvas.height / 2 - height / 2 + 1, 2, height);
		}
	}
	
	/* Get data URL of canvas. */
	var dataURL = canvas.toDataURL && canvas.toDataURL();
	
	/* Return CSS data URL. */
	return 'url(' + (dataURL || '') + ')';
};

/* Formats seconds as a human readable mm:ss string. */
function formatSeconds(s){	
	var minutes = Math.floor(s / 60);
	var seconds = Math.round(s % 60);
	
	minutes = (minutes < 10) ? '0' + minutes : minutes;
	seconds = (seconds < 10) ? '0' + seconds : seconds;
	
	return minutes + ':' + seconds;
};

/* Truncates a string to the specified length and appends '...'. */
String.prototype.truncate = function(length, suffix){
	length = length || 30;
	suffix = suffix || '...';
	
	if(length > 0 && this.length > length){
		return this.substring(0, length).concat(suffix);
	}
	
	return this;
};

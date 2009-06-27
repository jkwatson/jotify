/*
 *
 * Copyright (c) 2009, Felix Bruns <felixbruns@web.de>
 *
 */

function Jotify(options){
	/* Set default values for required options. */
	var gateway = options.gateway || 'http://localhost:8080';
	
	/* Set gateway. */
	this.setGateway = function(_gateway){
		gateway = _gateway;
	};
	
	/* Gateway request. */
	var request = function(handler, params, callbacks){
		/* Set default values. */
		params    = params    || {};
		callbacks = callbacks || {};
		
		/* Set format. */
		params.format = 'json';
		
		/* Request data. */
		$.ajax({
			data     : params,
			dataType : 'json',
			cache    : false,
			timeout  : 10000,
			type     : 'GET',
			url      : gateway + '/' + handler,
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
	
	/* Start. */
	this.start = function(callbacks){
		request('start', {}, callbacks);
	};
	
	/* Check. */
	this.check = function(session, callbacks){
		request('check', {session: session}, callbacks);
	};
	
	/* Login. */
	this.login = function(params, callbacks){
		request('login', params, callbacks);
	};
	
	/* Close. */
	this.close = function(session, callbacks){
		request('close', {session: session}, callbacks);
	};
	
	/* User. */
	this.user = function(session, callbacks){
		request('user', {session: session}, callbacks);
	};
	
	/* Toplist. */
	this.toplist = function(params, session, callbacks){
		params.session = session;
		
		request('toplist', params, callbacks);
	};
	
	/* Search. */
	this.search = function(params, session, callbacks){
		params.session = session;
		
		request('search', params, callbacks);
	};
	
	/* Image. */
	this.image = function(id, session){
		return gateway + '/image?session=' + session + '&id=' + id;
	};
	
	/* Browse. */
	this.browse = function(params, session, callbacks){
		params.session = session;
		
		request('browse', params, callbacks);
	};
	
	/* Playlist. */
	this.playlist = function(params, session, callbacks){
		params.session = session;
		
		request('playlist', params, callbacks);
	};
	
	/* Stream. */
	this.stream = function(track, session){
		return gateway + '/stream?session=' + session + '&id=' + track.id + '&file=' + track.files.file.id;
	};
}

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

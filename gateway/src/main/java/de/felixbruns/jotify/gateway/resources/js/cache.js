/*
 *
 * Copyright (c) 2009, Felix Bruns <felixbruns@web.de>
 *
 */

function Cache(name){
	/* Set default values for parameters. */
	this.name = name || 'default';
	
	/* Create cache if it doesn't exist. */
	if(typeof(localStorage[this.name]) == 'undefined' || localStorage[this.name] == null){
		localStorage[this.name] = '{}';
	}
}

/* Clear. */
Cache.prototype.clear = function(){
	localStorage[this.name] = '{}';
};

/* Contains. */
Cache.prototype.contains = function(hash){
	var cache = JSON.parse(localStorage[this.name]);
	
	return typeof(cache[hash]) != 'undefined';
};

/* Load. */
Cache.prototype.load = function(hash){
	var cache = JSON.parse(localStorage[this.name]);
	
	return cache[hash];
};

/* Load all. */
Cache.prototype.loadAll = function(){
	return JSON.parse(localStorage[this.name]);
};

/* Remove. */
Cache.prototype.remove = function(hash){
	var cache = JSON.parse(localStorage[this.name]);
	
	delete cache[hash];
	
	localStorage[this.name] = JSON.stringify(cache);
};

/* Store. */
Cache.prototype.store = function(hash, data){
	var cache = JSON.parse(localStorage[this.name]);
	
	cache[hash] = data;
	
	localStorage[this.name] = JSON.stringify(cache);
};

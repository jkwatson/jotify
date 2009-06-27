package de.felixbruns.jotify.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link Cache} implementation that stores data in memory.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class MemoryCache implements Cache {
	/**
	 * A key-value store holding cache data.
	 */
	private Map<String, byte[]> data = new HashMap<String, byte[]>();
	
	/**
	 * Create a new {@link MemoryCache}.
	 */
	public MemoryCache(){
		this.data = new HashMap<String, byte[]>();
	}
	
	/**
	 * Clear the entire cache.
	 */
	public void clear(){
		this.data.clear();
	}
	
	/**
	 * Clear the cache for the specified category.
	 * 
	 * @param category A cache category.
	 */
	public void clear(String category){
		for(String key : this.data.keySet()){
			if(key.startsWith(category + "-")){
				this.data.remove(key);
			}
		}
	}
	
	/**
	 * Check if the cache contains an item. 
	 * 
	 * @param category The cache category to check.
	 * @param hash     The hash of the item to check.
	 * 
	 * @return true if it contains that item, false otherwise.
	 */
	public boolean contains(String category, String hash){
		return this.data.containsKey(category + "-" + hash);
	}
	
	/**
	 * Load data from the cache.
	 * 
	 * @param category The cache category to load from.
	 * @param hash     The hash of the item to load.
	 * 
	 * @return Cached data or {@code null}.
	 */
	public byte[] load(String category, String hash){
		return this.data.get(category + "-" + hash);
	}
	
	/**
	 * Remove a single item from the cache.
	 * 
	 * @param category The cache category to remove from.
	 * @param hash     The hash of the item to remove.
	 */
	public void remove(String category, String hash){
		this.data.remove(category + "-" + hash);
	}
	
	/**
	 * Store data in the cache.
	 * 
	 * @param category The cache category to store to.
	 * @param hash     The hash of the item to store.
	 * @param data     The data to store.
	 */
	public void store(String category, String hash, byte[] data){
		this.store(category, hash, data, data.length);
	}
	
	/**
	 * Store data in the cache.
	 * 
	 * @param category The cache category to store to.
	 * @param hash     The hash of the item to store.
	 * @param data     The data to store.
	 * @param size     The size of the data.
	 */
	public void store(String category, String hash, byte[] data, int size){
		this.data.put(category + "-" + hash, Arrays.copyOf(data, size));
	}
	
	
	/**
	 * List data in a cache category.
	 * 
	 * @param category The cache category to list.
	 * 
	 * @return A {@link List} of cache hashes.
	 */
	public List<String> list(String category){
		List<String> hashes = new ArrayList<String>();
		
		for(String key : this.data.keySet()){
			if(key.startsWith(category + "-")){
				hashes.add(key.replaceFirst(category + "-", ""));
			}
		}
		
		return hashes;
	}
}

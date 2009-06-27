package de.felixbruns.jotify.cache;

import java.util.List;

/**
 * Interface for cache implementations.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public interface Cache {
	/**
	 * Clear the entire cache.
	 */
	public void clear();
	
	/**
	 * Clear the cache for the specified category.
	 * 
	 * @param category A cache category to clear.
	 */
	public void clear(String category);
	
	/**
	 * Check if the cache contains an item. 
	 * 
	 * @param category The cache category to check.
	 * @param hash     The hash of the item to check.
	 * 
	 * @return {@code true} if it contains that item, {@code false} otherwise.
	 */
	public boolean contains(String category, String hash);
	
	/**
	 * Load data from the cache.
	 * 
	 * @param category The cache category to load from.
	 * @param hash     The hash of the item to load.
	 * 
	 * @return Cached data or {@code null}.
	 */
	public byte[] load(String category, String hash);
	
	/**
	 * Remove a single item from the cache.
	 * 
	 * @param category The cache category to remove from.
	 * @param hash     The hash of the item to remove.
	 */
	public void remove(String category, String hash);
	
	/**
	 * Store data in the cache.
	 * 
	 * @param category The cache category to store to.
	 * @param hash     The hash of the item to store.
	 * @param data     The data to store.
	 */
	public void store(String category, String hash, byte[] data);
	
	/**
	 * Store data in the cache.
	 * 
	 * @param category The cache category to store to.
	 * @param hash     The hash of the item to store.
	 * @param data     The data to store.
	 * @param size     The size of the data.
	 */
	public void store(String category, String hash, byte[] data, int size);
	
	/**
	 * List data in a cache category.
	 * 
	 * @param category The cache category to list.
	 * 
	 * @return A {@link List} of cache hashes.
	 */
	public List<String> list(String category);
}

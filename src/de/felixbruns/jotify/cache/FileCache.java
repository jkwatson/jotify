package de.felixbruns.jotify.cache;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Cache} implementation that stores data in the filesystem.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class FileCache implements Cache {
	/**
	 * The directory for storing cache data.
	 */
	private File directory;
	
	/**
	 * Create a new {@link FileCache} with a default directory.
	 * The directory will be the value of the jotify.cache system
	 * property or '$HOME/.jotify-cache' if that property is
	 * undefined.
	 */
	public FileCache(){
		this(new File(
			System.getProperty("jotify.cache", 
			System.getProperty("user.home") + "/.jotify-cache")));
	}
	
	/**
	 * Create a new {@link FileCache} with a specified directory.
	 * If the directory doesn't exist, it will be created.
	 * 
	 * @param directory The directory to use for storing cache data.
	 */
	public FileCache(File directory){
		this.directory = directory;
		
		/* Create directory if it doesn't exists. */
		if(!this.directory.exists()){
			this.directory.mkdirs();
		}
	}
	
	/**
	 * Clear the entire cache.
	 */
	public void clear(){
		/* Loop over all files in the directory and delete them. */
		for(File file : this.directory.listFiles()){
			file.delete();
		}
	}
	
	/**
	 * Clear the cache for the specified category.
	 * 
	 * @param category A cache category.
	 */
	public void clear(String category){
		/* Get subdirectory of the specified category. */
		File directory = new File(this.directory, category);
		
		/* Loop over all files in that directory and delete them. */
		for(File file : directory.listFiles()){
			if(file.isFile()){
				file.delete();
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
		return new File(this.directory, category + "/" + hash).exists();
	}
	
	/**
	 * Load data from the cache. If an excetion occurs while reading the data
	 * from the file, {@code null} will be returned.
	 * 
	 * @param category The cache category to load from.
	 * @param hash     The hash of the item to load.
	 * 
	 * @return Cached data or {@code null}.
	 */
	public byte[] load(String category, String hash){
		try{
			/* Get input stream of data and allocate buffer. */
			File            file  = new File(this.directory, category + "/" + hash);
			FileInputStream input = new FileInputStream(file);
			byte[]          data  = new byte[(int)file.length()];
			
			/* Read input stream into buffer. */
			input.read(data);
			input.close();
			
			return data;
		}
		catch(IOException e){
			return null;
		}
	}
	
	/**
	 * Remove a single item from the cache.
	 * 
	 * @param category The cache category to remove from.
	 * @param hash     The hash of the item to remove.
	 */
	public void remove(String category, String hash){
		new File(this.directory, category + "/" + hash).delete();
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
	 * Store data in the cache. If an exception occurs while writing the data
	 * to the file it will be ignored.
	 * 
	 * @param category The cache category to store to.
	 * @param hash     The hash of the item to store.
	 * @param data     The data to store.
	 * @param size     The size of the data.
	 */
	public void store(String category, String hash, byte[] data, int size){
		try{
			/* Create file to hold the data. */
			File file = new File(this.directory, category + "/" + hash);
			
			/* Create directory path if necessary. */
			if(!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			
			/* Create output stream for file. */
			FileOutputStream output = new FileOutputStream(file);
			
			/* Write data to output stream. */
			output.write(data, 0, size);
			output.close();
		} 
		catch(IOException e){
			/* Ignore. */
		}
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
		
		/* Get subdirectory of the specified category. */
		File directory = new File(this.directory, category);
		
		/* Loop over all files in that directory. */
		for(File file : directory.listFiles()){
			if(file.isFile()){
				hashes.add(file.getName());
			}
		}
		
		return hashes;
	}
}

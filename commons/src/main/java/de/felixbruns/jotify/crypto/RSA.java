package de.felixbruns.jotify.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Class providing convenience methods for generating RSA key pairs.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class RSA {
	/**
	 * {@link KeyPairGenerator} object for creating new key pairs.
	 */
	private static KeyPairGenerator keyPairGenerator;
	
	/**
	 * {@link RSA} instance for creating new {@link RSAKeyPair} objects.
	 */
	private static RSA instance;
	
	/**
	 * Statically instantiate needed objects and create a class instance.
	 */
	static{
		try{
			keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
		
		/* Create RSA instance for creating new RSAKeyPair objects. */
		instance = new RSA();
	}
	
	/**
	 * Generate a key pair with the specified key size.
	 * 
	 * @param keysize The key size in bits.
	 * 
	 * @return A {@link RSAKeyPair} holding RSA private and public keys.
	 */
	public static RSAKeyPair generateKeyPair(int keysize){
		/* Check if key pair generator is instantiated. */
		if(keyPairGenerator == null){
			throw new RuntimeException("KeyPairGenerator not instantiated!");
		}
		
		/* Initialize key pair generator with keysize in bits. */
		keyPairGenerator.initialize(keysize);
		
		/* Generate key pair. */
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		
		/* Return key pair. */
		return instance.new RSAKeyPair(keyPair);
	}
	
	/**
	 * Convert a {@link RSAKey} to a byte array. Cuts-off a
	 * leading zero-byte if key length is not divisible by 8.
	 * 
	 * @param key The {@link RSAKey} to convert.
	 * 
	 * @return A byte array representation of the key or {@code null}.
	 */
	public static byte[] keyToBytes(RSAKey key){
		byte[] bytes = key.getModulus().toByteArray();
		
		/* Cut-off leading zero-byte if key length is not divisible by 8. */
		if(bytes.length % 8 != 0 && bytes[0] == 0x00){
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}
		
		return bytes;
	}
	
	/**
	 * A class holding RSA private and public keys.
	 * 
	 * @author Felix
	 *
	 * @category Crypto
	 */
	public class RSAKeyPair {
		/**
		 * A {@link RSAPrivateKey}.
		 */
		private RSAPrivateKey privateKey;
		
		/**
		 * A {@link RSAPublicKey}.
		 */
		private RSAPublicKey publicKey;
		
		/**
		 * Create a new {@link RSAKeyPair} using a private and public key.
		 * 
		 * @param privateKey The private key.
		 * @param publicKey  The public key.
		 */
		public RSAKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey){
			this.privateKey = privateKey;
			this.publicKey  = publicKey;
		}
		
		/**
		 * Create a new {@link RSAKeyPair} using a {@link KeyPair}.
		 * 
		 * @param keyPair The {@link KeyPair} object.
		 */
		public RSAKeyPair(KeyPair keyPair){			
			this((RSAPrivateKey)keyPair.getPrivate(), (RSAPublicKey)keyPair.getPublic());
		}
		
		/**
		 * Get the private key.
		 * 
		 * @return A {@link RSAPrivateKey} object.
		 */
		public RSAPrivateKey getPrivateKey(){
			return this.privateKey;
		}
		
		/**
		 * Get the private key as a byte array.
		 * 
		 * @return A byte array representation of the private key.
		 */
		public byte[] getPrivateKeyBytes(){
			return keyToBytes(this.privateKey);
		}
		
		/**
		 * Get the public key.
		 * 
		 * @return A {@link RSAPublicKey} object.
		 */
		public RSAPublicKey getPublicKey(){
			return this.publicKey;
		}
		
		/**
		 * Get the public key as a byte array.
		 * 
		 * @return A byte array representation of the public key.
		 */
		public byte[] getPublicKeyBytes(){
			return keyToBytes(this.publicKey);
		}
	}
}

package de.felixbruns.jotify.crypto;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPrivateKeySpec;
import javax.crypto.spec.DHPublicKeySpec;

/**
 * Class providing convenience methods for generating Diffie-Hellman
 * key pairs and computing shared keys.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class DH {
	/**
	 * {@link KeyPairGenerator} object for creating new key pairs.
	 */
	private static KeyPairGenerator keyPairGenerator;
	
	/**
	 * {@link KeyAgreement} object for computing shared keys.
	 */
	private static KeyAgreement keyAgreement;
	
	/**
	 * {@link KeyFactory} object for creating keys from bytes.
	 */
	private static KeyFactory keyFactory;
	
	/**
	 * {@link DH} instance for creating new {@link DHKeyPair} objects.
	 */
	private static DH instance;
	
	/**
	 * Generator to use for key generation.
	 */
	private static BigInteger generator = new BigInteger("2");
	
	/**
	 * Prime number to use for key generation.
	 * Well-known Group 1, 768-bit prime.
	 */
	private static BigInteger prime = bytesToBigInteger(new byte[]{
		(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xc9, (byte)0x0f, (byte)0xda, (byte)0xa2, (byte)0x21, (byte)0x68, (byte)0xc2, (byte)0x34,
		(byte)0xc4, (byte)0xc6, (byte)0x62, (byte)0x8b, (byte)0x80, (byte)0xdc, (byte)0x1c, (byte)0xd1,
		(byte)0x29, (byte)0x02, (byte)0x4e, (byte)0x08, (byte)0x8a, (byte)0x67, (byte)0xcc, (byte)0x74,
		(byte)0x02, (byte)0x0b, (byte)0xbe, (byte)0xa6, (byte)0x3b, (byte)0x13, (byte)0x9b, (byte)0x22,
		(byte)0x51, (byte)0x4a, (byte)0x08, (byte)0x79, (byte)0x8e, (byte)0x34, (byte)0x04, (byte)0xdd,
		(byte)0xef, (byte)0x95, (byte)0x19, (byte)0xb3, (byte)0xcd, (byte)0x3a, (byte)0x43, (byte)0x1b,
		(byte)0x30, (byte)0x2b, (byte)0x0a, (byte)0x6d, (byte)0xf2, (byte)0x5f, (byte)0x14, (byte)0x37,
		(byte)0x4f, (byte)0xe1, (byte)0x35, (byte)0x6d, (byte)0x6d, (byte)0x51, (byte)0xc2, (byte)0x45,
		(byte)0xe4, (byte)0x85, (byte)0xb5, (byte)0x76, (byte)0x62, (byte)0x5e, (byte)0x7e, (byte)0xc6,
		(byte)0xf4, (byte)0x4c, (byte)0x42, (byte)0xe9, (byte)0xa6, (byte)0x3a, (byte)0x36, (byte)0x20,
		(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
	});
	
	/**
	 * Statically instantiate needed objects and create a class instance.
	 */
	static{
		try{
			keyPairGenerator = KeyPairGenerator.getInstance("DH");
			keyAgreement     = KeyAgreement.getInstance("DH");
			keyFactory       = KeyFactory.getInstance("DH");
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException(e);
		}
		
		/* Create DH instance for creating new DHKeyPair objects. */
		instance = new DH();
	}
	
	/**
	 * Generate a key pair with the specified key size.
	 * 
	 * @param keysize The key size in bits.
	 * 
	 * @return A {@link DHKeyPair} holding Diffie-Hellman private and public keys.
	 */
	public static DHKeyPair generateKeyPair(int keysize){
		/* Check if key pair generator is instantiated. */
		if(keyPairGenerator == null){
			throw new RuntimeException("KeyPairGenerator not instantiated!");
		}
		
		/* Initialize key pair generator with prime, generator and keysize in bits. */
		try{
			keyPairGenerator.initialize(
				new DHParameterSpec(prime, generator, keysize)
			);
		}
		catch(InvalidAlgorithmParameterException e){
			throw new RuntimeException(e);
		}
		
		/* Generate key pair. */
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		
		/* Return key pair. */
		return instance.new DHKeyPair(keyPair);
	}
	
	/**
	 * Compute a shared key using a private and a public key.
	 * 
	 * @param privateKey A {@link DHPrivateKey} object.
	 * @param publicKey  A {@link DHPublicKey} object.
	 * 
	 * @return The shared key as a byte array.
	 */
	public static byte[] computeSharedKey(DHPrivateKey privateKey, DHPublicKey publicKey){
		/* Check if key agreement is instantiated. */
		if(keyAgreement == null){
			throw new RuntimeException("KeyAgreement not instantiated!");
		}
		
		/* Initialize key agreement with private key and execute next phase with public key. */
		try{
			keyAgreement.init(privateKey);
			keyAgreement.doPhase(publicKey, true);
		}
		catch(InvalidKeyException e){
			throw new RuntimeException(e);
		}
		
		/* Return shared key. */
		return keyAgreement.generateSecret();
	}
	
	/**
	 * Convert a byte array to a {@link BigInteger}.
	 * Adds a leading zero-byte to ensure a positive {@link BigInteger}.
	 * 
	 * @param bytes The byte array to convert.
	 * 
	 * @return A {@link BigInteger} object.
	 */
	public static BigInteger bytesToBigInteger(byte[] bytes){
		/* Pad with 0x00 so we don't get a negative BigInteger!!! */
		ByteBuffer key = ByteBuffer.allocate(bytes.length + 1);
		
		key.put((byte)0x00);
		key.put(bytes);
		
		return new BigInteger(key.array());
	}
	
	/**
	 * Convert a {@link DHKey} to a byte array. Uses X or Y values
	 * of a key depending on key type (private or public). Cuts-off
	 * a leading zero-byte if key length is not divisible by 8.
	 * 
	 * @param key The {@link DHKey} to convert.
	 * 
	 * @return A byte array representation of the key or {@code null}.
	 */
	public static byte[] keyToBytes(DHKey key){
		byte[] bytes = null;
		
		/* Check key type and use appropriate value. */
		if(key instanceof DHPublicKey){
			bytes = ((DHPublicKey)key).getY().toByteArray();
		}
		else if(key instanceof DHPrivateKey){
			bytes = ((DHPrivateKey)key).getX().toByteArray();
		}
		
		/* Return null on failure. */
		if(bytes == null){
			return null;
		}
		
		/* Cut-off leading zero-byte if key length is not divisible by 8. */
		if(bytes.length % 8 != 0 && bytes[0] == 0x00){
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
		}
		
		return bytes;
	}
	
	/**
	 * Create a {@link DHPublicKey} from a byte array.
	 * 
	 * @param parameterSpec The {@link DHParameterSpec} to use.
	 * @param bytes         The key bytes.
	 * 
	 * @return A {@link DHPublicKey} object.
	 */
	public static DHPublicKey bytesToPublicKey(DHParameterSpec parameterSpec, byte[] bytes){
		/* Set Y (public key), P and G values. */
		KeySpec keySpec = new DHPublicKeySpec(
			bytesToBigInteger(bytes),
			parameterSpec.getP(),
			parameterSpec.getG()
		);
		
		/* Generate public key from key spec */
		try{
			return (DHPublicKey)keyFactory.generatePublic(keySpec);
		}
		catch(InvalidKeySpecException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Create a {@link DHPrivateKey} from a byte array.
	 * 
	 * @param parameterSpec The {@link DHParameterSpec} to use.
	 * @param bytes         The key bytes.
	 * 
	 * @return A {@link DHPrivateKey} object.
	 */
	public static DHPrivateKey bytesToPrivateKey(DHParameterSpec parameterSpec, byte[] bytes){
		/* Set X (private key), P and G values. */
		KeySpec keySpec = new DHPrivateKeySpec(
			bytesToBigInteger(bytes),
			parameterSpec.getP(),
			parameterSpec.getG()
		);
		
		/* Generate private key from key spec */
		try{
			return (DHPrivateKey)keyFactory.generatePrivate(keySpec);
		}
		catch(InvalidKeySpecException e){
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * A class holding Diffie-Hellman private and public keys.
	 * 
	 * @author Felix
	 *
	 * @category Crypto
	 */
	public class DHKeyPair {
		/**
		 * A {@link DHPrivateKey}.
		 */
		private DHPrivateKey privateKey;

		/**
		 * A {@link DHPublicKey}.
		 */
		private DHPublicKey publicKey;
		
		/**
		 * Create a new {@link DHKeyPair} using a private and public key.
		 * 
		 * @param privateKey The private key.
		 * @param publicKey  The public key.
		 */
		public DHKeyPair(DHPrivateKey privateKey, DHPublicKey publicKey){
			this.privateKey = privateKey;
			this.publicKey  = publicKey;
		}
		
		/**
		 * Create a new {@link DHKeyPair} using a {@link KeyPair}.
		 * 
		 * @param keyPair The {@link KeyPair} object.
		 */
		public DHKeyPair(KeyPair keyPair){			
			this((DHPrivateKey)keyPair.getPrivate(), (DHPublicKey)keyPair.getPublic());
		}
		
		/**
		 * Get the private key.
		 * 
		 * @return A {@link DHPrivateKey} object.
		 */
		public DHPrivateKey getPrivateKey(){
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
		 * @return A {@link DHPublicKey} object.
		 */
		public DHPublicKey getPublicKey(){
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

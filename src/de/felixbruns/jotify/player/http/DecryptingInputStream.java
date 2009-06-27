package de.felixbruns.jotify.player.http;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptingInputStream extends InputStream {
	private InputStream stream;
	
	private Cipher cipher;
	private Key    key;
	private byte[] iv;
	
	private byte[] buffer;
	private int    fill;
	
	public DecryptingInputStream(InputStream stream, byte[] key){
		if(stream == null){
			throw new IllegalArgumentException("You need to supply a stream!");
		}
		
		this.stream = stream;
		
		/* Initialize AES cipher. */		
		try{
			/* Get AES cipher instance. */
			this.cipher = Cipher.getInstance("AES/CTR/NoPadding");
			
			/* Create secret key from bytes and set initial IV. */
			this.key = new SecretKeySpec(key, "AES");
			this.iv  = new byte[]{
				(byte)0x72, (byte)0xe0, (byte)0x67, (byte)0xfb,
				(byte)0xdd, (byte)0xcb, (byte)0xcf, (byte)0x77,
				(byte)0xeb, (byte)0xe8, (byte)0xbc, (byte)0x64,
				(byte)0x3f, (byte)0x63, (byte)0x0d, (byte)0x93
			};
			
			/* Initialize cipher with key and IV in encrypt mode. */
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(this.iv));
		}
		catch(NoSuchAlgorithmException e){
			throw new RuntimeException("AES/CTR is not available!", e);
		}
		catch(NoSuchPaddingException e){
			throw new RuntimeException("'NoPadding' is not available! mmh. yeah.", e);
		}
		catch (InvalidKeyException e){
			throw new RuntimeException("Invalid key!", e);
		}
		catch (InvalidAlgorithmParameterException e){
			throw new RuntimeException("Invalid IV!", e);
		}
		
		this.buffer = new byte[16];
		this.fill   = 0;
	}
	
	private int readBlock() throws IOException {
		/* Read next 16 bytes of data. */
		return this.stream.read(this.buffer, 0, 16);
	}
	
	private void decryptBlock() throws IOException {
		byte[] keystream;
		
		/* Produce 16 bytes of keystream from the IV. */
		try{
			keystream = this.cipher.doFinal(this.iv);
		}
		catch(IllegalBlockSizeException e){
			throw new RuntimeException("Illegal block size!", e);
		}
		catch(BadPaddingException e){
			throw new RuntimeException("Bad padding!", e);
		}
		
		/* 
		 * Produce plaintext by XORing ciphertext with keystream.
		 * And somehow I also need to XOR with the IV... Please
		 * somebody tell me what I'm doing wrong, or is it the
		 * Java implementation of AES? At least it works like this.
		 */
		for(int i = 0; i < 16; i++){
			this.buffer[i] ^= keystream[i] ^ this.iv[i];
		}
	}
	
	private void updateIV(){
		/* Update IV counter. */
		for(int i = 15; i >= 0; i--){
			this.iv[i] += 1;
			
			if((int)(this.iv[i] & 0xFF) != 0){
				break;
			}
		}
		
		/* Set new IV. */
		try{
			this.cipher.init(Cipher.ENCRYPT_MODE, this.key, new IvParameterSpec(this.iv));
		}
		catch(InvalidKeyException e){
			e.printStackTrace();
		}
		catch(InvalidAlgorithmParameterException e){
			e.printStackTrace();
		}
	}
	
	public int read() throws IOException {
		/* Check if we need to read more data. */
		if(this.fill <= 0){
			if(this.readBlock() == 0){
				return -1;				
			}
			
			this.decryptBlock();
			this.updateIV();
			
			this.fill = 16;
		}
		
		int value = (int)(this.buffer[16 - this.fill] & 0xFF);
		
		this.fill--;
		
		return value;
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		int read = 0;
		int value;
		
		for(int i = off; i < off + len; i++, read++){
			if((value = this.read()) == -1){
				return read;
			}
			
			b[i] = (byte)value;
		}
		
		return read;
	}
	
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
}

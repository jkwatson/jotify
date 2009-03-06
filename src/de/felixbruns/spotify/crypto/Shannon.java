package de.felixbruns.spotify.crypto;

import java.util.Arrays;

public class Shannon {
	private static int N = 16;
	
	private int[] R;
	private int[] CRC;
	private int[] initR;
	private int   konst;
	private int   sbuf;
	private int   mbuf;
	private int   nbuf;
	
	public Shannon(){
		/* Registers with length N. */
		this.R     = new int[N];
		this.CRC   = new int[N];
		this.initR = new int[N];
	}
	
	/* Nonlinear transform (sbox) of a word. There are two slightly different combinations */
	private int sbox(int i){
		i ^= Integer.rotateLeft(i,  5) | Integer.rotateLeft(i,  7);
		i ^= Integer.rotateLeft(i, 19) | Integer.rotateLeft(i, 22);
		
		return i;
	}
	
	private int sbox2(int i){
		i ^= Integer.rotateLeft(i, 7) | Integer.rotateLeft(i, 22);
		i ^= Integer.rotateLeft(i, 5) | Integer.rotateLeft(i, 19);
		
		return i;
	}
	
	/* Cycle the contents of the register and calculate output word in sbuf. */
	private void cycle(){
		/* Temporary variable. */
		int t;
		
		/* Nonlinear feedback function. */
		t = this.R[12] ^ this.R[13] ^ this.konst;
		t = this.sbox(t) ^ Integer.rotateLeft(this.R[0], 1);
		
		/* Shift register. */
		for(int i = 1; i < N; ++i){
			this.R[i - 1] = this.R[i];
		}
		
		this.R[N - 1] = t;
			
		t          = sbox2(this.R[2] ^ this.R[15]);
		this.R[0] ^= t;
		this.sbuf  = t ^ this.R[8] ^ this.R[12];
	}
	
	/* 
	 * Accumulate a CRC of input words, later to be fed into MAC.
	 * This is actualy 32 parallel CRC-16s, using the IBM CRC-16
	 * polynomian x^16 + x^15 + x^2 + 1
	 */
	private void crcFunc(int i){
		/* Temporary variable. */
		int t;
		
		/* Accumulate CRC of input. */
		t = this.CRC[0] ^ this.CRC[2] ^ this.CRC[15] ^ i;
		
		for(int j = 1; j < N; j++){
			this.CRC[j - 1] = this.CRC[j];
		}
		
		this.CRC[N - 1] = t;
	}
	
	/* Normal MAC word processing: do both stream register and CRC. */
	private void macFunc(int i){
		this.crcFunc(i);
		
		this.addKey(i);
	}
	
	/* Initialize to known state. */
	private void initState(){
		/* Register initialized to Fibonacci numbers. */
		this.R[0] = 1;
		this.R[1] = 1;
		
		for(int i = 2; i < N; i++){
			this.R[i] = this.R[i - 1] + this.R[i - 2];
		}
		
		/* Initialization constant. */
		this.konst = 0x6996c53a;
	}
	
	/* Save the current register state. */
	private void saveState(){
		for(int i = 0; i < N; i++){
			this.initR[i] = this.R[i];
		}
	}
	
	/* Inisialize to previously saved register state. */
	private void reloadState(){
		for(int i = 0; i < N; i++){
			this.R[i] = this.initR[i];
		}
	}
	
	/* Initialize 'konst'. */
	private void genKonst(){
		this.konst = this.R[0];
	}
	
	/* Load key material into the register. */
	private void addKey(int k){
		this.R[13] ^= k;
	}
	
	/* Extra nonlinear diffusion of register for key and MAC. */
	private void diffuse(){
		for(int i = 0; i < N; i++){
			this.cycle();
		}
	}
	
	/*
	 * Common actions for loading key material.
	 * Allow non-word-multiple key and nonce material.
	 * Note: Also initializes the CRC register as a side effect.
	 */
	private void loadKey(byte[] key){
		byte[] extra = new byte[4];
		int i, j, k;
		
		/* Start folding key. */
		for(i = 0; i < (key.length & ~0x03); i += 4){
			/* Shift 4 bytes into one word. */
			k =	((key[i + 3] & 0xFF) << 24) |
				((key[i + 2] & 0xFF) << 16) |
				((key[i + 1] & 0xFF) << 8)  |
				((key[i    ] & 0xFF));
			
			/* Insert key word at index 13. */
			this.addKey(k);
			
			/* Cycle register. */
			this.cycle();
		}
		
		/* If there were any extra bytes, zero pad to a word. */
		if(i < key.length){
			/* i remains unchanged at start of loop. */
			for(j = 0; i < key.length; i++){
				extra[j++] = key[i];
			}
			
			/* j remains unchanged at start of loop. */
			for(; j < 4; j++){
				extra[j] = 0;
			}
			
			/* Shift 4 extra bytes into one word. */
			k =	((extra[3] & 0xFF) << 24) |
				((extra[2] & 0xFF) << 16) |
				((extra[1] & 0xFF) << 8)  |
				((extra[0] & 0xFF));
			
			/* Insert key word at index 13. */
			this.addKey(k);
			
			/* Cycle register. */
			this.cycle();
		}
		
		/* Also fold in the length of the key. */
		this.addKey(key.length);
		
		/* Cycle register. */
		this.cycle();
		
		/* Save a copy of the register. */
		this.CRC = Arrays.copyOf(this.R, N);
		
		/* Now diffuse. */
		this.diffuse();
		
		/* Now XOR the copy back -- makes key loading irreversible. */
		for(i = 0; i < N; i++){
			this.R[i] ^= this.CRC[i];
		}
	}
	
	/* Set key */
	public void key(byte[] key){
		/* Initializet known state. */
		this.initState();
		
		/* Load key material. */
		this.loadKey(key);
		
		/* In case we proceed to stream generation. */
		this.genKonst();
		
		/* Save register state. */
		this.saveState();
		
		/* Set 'nbuf' value to zero. */
		this.nbuf = 0;
	}
	
	/* Set "IV" */
	public void nonce(byte[] nonce){
		/* Reload register state. */
		this.reloadState();
		
		/* Set initialization constant. */
		this.konst = 0x6996c53a;
		
		/* Load "IV" material. */
		this.loadKey(nonce);
		
		/* Set 'konst'. */
		this.genKonst();
		
		/* Set 'nbuf' value to zero. */
		this.nbuf = 0;
	}
	
	/*
	 * XOR pseudo-random bytes into buffer.
	 * Note: doesn't play well with MAC functions. 
	 */
	public void stream(byte[] buffer){
		int i = 0, j, n = buffer.length;
		
		/* Handle any previously buffered bytes. */
		while(this.nbuf != 0 && n != 0){
			buffer[i++] ^= this.sbuf & 0xFF;
			
			this.sbuf >>= 8;
			this.nbuf  -= 8;
			
			n--;
		}
		
		/* Handle whole words. */
		j = n & ~0x03;
		
		while(i < j){
			/* Cycle register. */
			this.cycle();
			
			/* XOR word. */
			buffer[i + 3] ^= (this.sbuf >> 24) & 0xFF;
			buffer[i + 2] ^= (this.sbuf >> 16) & 0xFF;
			buffer[i + 1] ^= (this.sbuf >>  8) & 0xFF;
			buffer[i    ] ^= (this.sbuf      ) & 0xFF;
			
			i += 4;
		}
		
		/* Handle any trailing bytes. */
		n &= 0x03;
		
		if(n != 0){
			/* Cycle register. */
			this.cycle();
			
			this.nbuf = 32;
			
			while(this.nbuf != 0 && n != 0){
				buffer[i++] ^= this.sbuf & 0xFF;
				
				this.sbuf >>= 8;
				this.nbuf  -= 8;
				
				n--;
			}
		}
	}
	
	/*
	 * Accumulate words into MAC without encryption.
	 * Note that plaintext is accumulated for MAC.
	 */
	public void macOnly(byte[] buffer){
		int i = 0, j, k, n = buffer.length;
		
		/* Handle any previously buffered bytes. */
		if(this.nbuf != 0){
			while(this.nbuf != 0 && n != 0){
				this.mbuf ^= buffer[i++] << (32 - this.nbuf);
				this.nbuf -= 8;
				
				n--;
			}
			
			/* Not a whole word yet. */
			if(this.nbuf != 0){
				return;			
			}
			
			/* LFSR already cycled. */
			this.macFunc(this.mbuf);
		}
			
		/* Handle whole words. */
		j = n & ~0x03;
		
		while(i < j){
			/* Cycle register. */
			this.cycle();
			
			/* Shift 4 bytes into one word. */
			k =	((buffer[i + 3] & 0xFF) << 24) |
				((buffer[i + 2] & 0xFF) << 16) |
				((buffer[i + 1] & 0xFF) << 8)  |
				((buffer[i    ] & 0xFF));
			
			this.macFunc(k);
			
			i += 4;
		}
		
		/* Handle any trailing bytes. */
		n &= 0x03;
		
		if(n != 0){
			/* Cycle register. */
			this.cycle();
			
			this.mbuf = 0;
			this.nbuf = 32;
			
			while(this.nbuf != 0 && n != 0){
				this.mbuf ^= buffer[i++] << (32 - this.nbuf);
				this.nbuf  -= 8;
				
				n--;
			}
		}
	}
	
	/*
	 * Combined MAC and encryption.
	 * Note that plaintext is accumulated for MAC.
	 */
	public void encrypt(byte[] buffer){
		this.encrypt(buffer, buffer.length);
	}
	
	/*
	 * Combined MAC and encryption.
	 * Note that plaintext is accumulated for MAC.
	 */
	public void encrypt(byte[] buffer, int n){
		int i = 0, j, k;
		
		/* Handle any previously buffered bytes. */
		if(this.nbuf != 0){
			while(this.nbuf != 0 && n != 0){
				this.mbuf ^= buffer[i]  << (32 - this.nbuf);
				buffer[i] ^= (this.sbuf >> (32 - this.nbuf)) & 0xFF;
				
				i++;
				
				this.nbuf -= 8;
				
				n--;
			}
			
			/* Not a whole word yet. */
			if(this.nbuf != 0){
				return;			
			}
			
			/* LFSR already cycled. */
			this.macFunc(this.mbuf);
		}
		
		/* Handle whole words. */
		j = n & ~0x03;
		
		while(i < j){
			/* Cycle register. */
			this.cycle();
			
			/* Shift 4 bytes into one word. */
			k =	((buffer[i + 3] & 0xFF) << 24) |
				((buffer[i + 2] & 0xFF) << 16) |
				((buffer[i + 1] & 0xFF) << 8)  |
				((buffer[i    ] & 0xFF));
			
			this.macFunc(k);
			
			k ^= this.sbuf;
			
			/* Put word into byte buffer. */
			buffer[i + 3] = (byte)((k >> 24) & 0xFF);
			buffer[i + 2] = (byte)((k >> 16) & 0xFF);
			buffer[i + 1] = (byte)((k >>  8) & 0xFF);
			buffer[i    ] = (byte)((k      ) & 0xFF);
			
			i += 4;
		}
		
		/* Handle any trailing bytes. */
		n &= 0x03;
		
		if(n != 0){
			/* Cycle register. */
			this.cycle();
			
			this.mbuf = 0;
			this.nbuf = 32;
			
			while(this.nbuf != 0 && n != 0){
				this.mbuf ^= buffer[i]  << (32 - this.nbuf);
				buffer[i] ^= (this.sbuf >> (32 - this.nbuf)) & 0xFF;
				
				i++;
				
				this.nbuf  -= 8;
				
				n--;
			}
		}
	}
	
	/*
	 * Combined MAC and decryption.
	 * Note that plaintext is accumulated for MAC.
	 */
	public void decrypt(byte[] buffer){
		this.decrypt(buffer, buffer.length);
	}
	
	/*
	 * Combined MAC and decryption.
	 * Note that plaintext is accumulated for MAC.
	 */
	public void decrypt(byte[] buffer, int n){
		int i = 0, j, k;
		
		/* Handle any previously buffered bytes. */
		if(this.nbuf != 0){
			while(this.nbuf != 0 && n != 0){
				buffer[i] ^= (this.sbuf >> (32 - this.nbuf)) & 0xFF;
				this.mbuf ^= buffer[i]  << (32 - this.nbuf);
				
				i++;
				
				this.nbuf -= 8;
				
				n--;
			}
			
			/* Not a whole word yet. */
			if(this.nbuf != 0){
				return;			
			}
			
			/* LFSR already cycled. */
			this.macFunc(this.mbuf);
		}
		
		/* Handle whole words. */
		j = n & ~0x03;
		
		while(i < j){
			/* Cycle register. */
			this.cycle();
			
			/* Shift 4 bytes into one word. */
			k =	((buffer[i + 3] & 0xFF) << 24) |
				((buffer[i + 2] & 0xFF) << 16) |
				((buffer[i + 1] & 0xFF) << 8)  |
				((buffer[i    ] & 0xFF));
			
			k ^= this.sbuf;
			
			this.macFunc(k);
			
			/* Put word into byte buffer. */
			buffer[i + 3] = (byte)((k >> 24) & 0xFF);
			buffer[i + 2] = (byte)((k >> 16) & 0xFF);
			buffer[i + 1] = (byte)((k >>  8) & 0xFF);
			buffer[i    ] = (byte)((k      ) & 0xFF);
			
			i += 4;
		}
		
		/* Handle any trailing bytes. */
		n &= 0x03;
		
		if(n != 0){
			/* Cycle register. */
			this.cycle();
			
			this.mbuf = 0;
			this.nbuf = 32;
			
			while(this.nbuf != 0 && n != 0){
				buffer[i] ^= (this.sbuf >> (32 - this.nbuf)) & 0xFF;
				this.mbuf ^= buffer[i]  << (32 - this.nbuf);
				
				i++;
				
				this.nbuf  -= 8;
				
				n--;
			}
		}
	}
	
	/*
	 * Having accumulated a MAC, finish processing and return it.
	 * Note that any unprocessed bytesare treated as if they were
	 * encrypted zero bytes, so plaintext (zero) is accumulated.
	 */
	public void finish(byte[] buffer){
		this.finish(buffer, buffer.length);
	}
	
	/*
	 * Having accumulated a MAC, finish processing and return it.
	 * Note that any unprocessed bytesare treated as if they were
	 * encrypted zero bytes, so plaintext (zero) is accumulated.
	 */
	public void finish(byte[] buffer, int n){
		int i = 0, j;
		
		/* Handle any previously buffered bytes. */
		if(this.nbuf != 0){
			/* LFSR already cycled. */
			this.macFunc(this.mbuf);
		}
		
		/*
		 * Perturb the MAC to mark end of input.
		 * Note that only the stream register is updated, not the CRC.
		 * This is an action that can't be duplicated by passing in plaintext,
		 * hence defeating any kind of extension attack.
		 */
		this.cycle();
		this.addKey(0x6996c53a ^ (this.nbuf << 3));
		
		this.nbuf = 0;
		
		/* Now add the CRC to the stream register and diffuse it. */
		for(j = 0; j < N; ++j){
			this.R[j] ^= this.CRC[j];
		}
		
		this.diffuse();
		
		/* Produce output from the stream buffer. */
		while(n > 0){
			this.cycle();
			
			if(n >= 4){
				/* Put word into byte buffer. */
				buffer[i + 3] = (byte)((this.sbuf >> 24) & 0xFF);
				buffer[i + 2] = (byte)((this.sbuf >> 16) & 0xFF);
				buffer[i + 1] = (byte)((this.sbuf >>  8) & 0xFF);
				buffer[i    ] = (byte)((this.sbuf      ) & 0xFF);
				
				n -= 4;
				i += 4;
			}
			else{
				for(j = 0; j < n; j++){
					buffer[i + j] = (byte)((this.sbuf >> (i * 8)) & 0xFF);
				}
				
				break;
			}
		}
	}
	
	public static void main(String args[]){
		Shannon.selfTest();
	}
	
	public static void selfTest(){
		/* Cipher instance. */
		Shannon shannon = new Shannon();
		
		/* Key. */
		byte[] key = new byte[]{
			(byte)0xda, (byte)0x3c, (byte)0x70, (byte)0xf5, (byte)0x3a, (byte)0x89, (byte)0xa1, (byte)0x2c,
			(byte)0x36, (byte)0x9e, (byte)0x1a, (byte)0xca, (byte)0x5e, (byte)0xa0, (byte)0xdc, (byte)0x6f,
			(byte)0xb1, (byte)0x6c, (byte)0xe3, (byte)0x5f, (byte)0x14, (byte)0xb6, (byte)0xe6, (byte)0xbe,
			(byte)0xcf, (byte)0x73, (byte)0x86, (byte)0xc8, (byte)0x74, (byte)0xda, (byte)0xc0, (byte)0x69
		};
		
		/* Nonce. */
		byte[] nonce = new byte[]{
			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
		};
		
		/* Plaintext. */
		String dataString = "This is a secret text. And I need to make it longer. Longer. And even longer. And even longer. And longer.";
		byte[] dataBuffer = dataString.getBytes();
		
		/* Print info. */
		System.out.println("--------------------------------------------------");
		System.out.println("Shannon stream cipher self test:");
		System.out.println("Plaintext: " + dataString);
		
		/* Set key and nonce, then encrypt. */
		shannon.key(key);
		shannon.nonce(nonce);
		shannon.encrypt(dataBuffer);
		
		/* Print encrypted data. */
		System.out.println("Encrypted: " + new String(dataBuffer));
		
		
		/* Set key and nonce, then decrypt. */
		shannon.key(key);
		shannon.nonce(nonce);
		
		byte[] a = Arrays.copyOfRange(dataBuffer, 0, 3);
		shannon.decrypt(a);
		
		byte[] b = Arrays.copyOfRange(dataBuffer, 3, dataBuffer.length);
		shannon.decrypt(b);

		/* Print decrypted data. */
		System.out.println("Decrypted: " + new String(a) + new String(b));
		
		/* Check for success. */
		if((new String(a) + new String(b)).equals(dataString)){
			System.out.println("Self-test successful!");
		}
		else{
			System.err.println("Self-test failed!");
		}
		
		/* Print end marker. */
		System.out.println("--------------------------------------------------");
	}
}

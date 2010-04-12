package de.felixbruns.jotify.player;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Decodes the Spotify specific OGG header and provides that information.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class SpotifyOggHeader {
	private int   samples;
	private int   length;
	private int   unknown;
	private int[] table;
	private float gainScale;
	private float gainDb;
	
	private static int[] headerTableDec = new int[]{
		    0,    112,    197,    327,    374,    394,    407,    417,
		  425,    433,    439,    444,    449,    454,    458,    462,
		  466,    470,    473,    477,    480,    483,    486,    489,
		  491,    494,    497,    499,    502,    504,    506,    509,
		  511,    513,    515,    517,    519,    521,    523,    525,
		  527,    529,    531,    533,    535,    537,    538,    540,
		  542,    544,    545,    547,    549,    550,    552,    554,
		  555,    557,    558,    560,    562,    563,    565,    566,
		  568,    569,    571,    572,    574,    575,    577,    578,
		  580,    581,    583,    584,    585,    587,    588,    590,
		  591,    593,    594,    595,    597,    598,    599,    601,
		  602,    604,    605,    606,    608,    609,    610,    612,
		  613,    615,    616,    617,    619,    620,    621,    623,
		  624,    625,    627,    628,    629,    631,    632,    634,
		  635,    636,    638,    639,    640,    642,    643,    644,
		  646,    647,    649,    650,    651,    653,    654,    655,
		  657,    658,    660,    661,    662,    664,    665,    667,
		  668,    669,    671,    672,    674,    675,    677,    678,
		  679,    681,    682,    684,    685,    687,    688,    690,
		  691,    693,    694,    696,    697,    699,    700,    702,
		  704,    705,    707,    708,    710,    712,    713,    715,
		  716,    718,    720,    721,    723,    725,    727,    728,
		  730,    732,    734,    735,    737,    739,    741,    743,
		  745,    747,    748,    750,    752,    754,    756,    758,
		  760,    763,    765,    767,    769,    771,    773,    776,
		  778,    780,    782,    785,    787,    790,    792,    795,
		  797,    800,    803,    805,    808,    811,    814,    817,
		  820,    823,    826,    829,    833,    836,    840,    843,
		  847,    851,    855,    859,    863,    868,    872,    877,
		  882,    887,    893,    898,    904,    911,    918,    925,
		  933,    941,    951,    961,    972,    985,   1000,   1017,
		 1039,   1067,   1108,   1183,   1520,   2658,   4666,   8191
	};
	
	private SpotifyOggHeader(){
		this.samples   = 0;
		this.length    = 0;
		this.unknown   = 0;
		this.table     = new int[0];
		this.gainScale = 1.0f;
		this.gainDb    = 0.0f;
	}
	
	/* Total number of samples. */
	public int getSamples(){
		return this.samples;
	}
	
	/**
	 * Return the length in milliseconds contained in this header.
	 * 
	 * @return The length in milliseconds contained in this header.
	 */
	public int getLength(int sampleRate){
		return this.samples / (sampleRate / 1000);
	}
	
	/**
	 * Return the length in bytes contained in this header.
	 * 
	 * @return The length in bytes contained in this header.
	 */
	public int getBytes(){
		return this.length;
	}
	
	public float getGainScale(){
		return this.gainScale;
	}
	
	public float getGainDb(){
		return this.gainDb;
	}
	
	/* Swap short bytes. */
	private static short swap(short value){
		return (short)(((value & 0x00ffL) << 8) |
					   ((value & 0xff00L) >> 8));
	}
	
	/* Swap integer bytes. */
	private static int swap(int value){
		return (int)(((value & 0x000000ffL) << 24) |
					 ((value & 0x0000ff00L) <<  8) |
					 ((value & 0x00ff0000L) >>  8) |
					 ((value & 0xff000000L) >> 24));
	}
	
	/* Decode Spotify OGG header. */
	public static SpotifyOggHeader decode(byte[] header) throws IOException {
		/* Create instance. */
		SpotifyOggHeader decoded = new SpotifyOggHeader();
		
		/* Create DataInputStream from InputStream. */
		DataInputStream input = new DataInputStream(new ByteArrayInputStream(header));
		
		/* Skip OGG page header (length is always 0x1C in this case). */
		input.skip(0x1C);
		
		/* Read Spotify specific data. */
		if(input.read() == 0x81){
			while(input.available() >= 2){
				int blockSize = swap(input.readShort());
				
				if(input.available() >= blockSize && blockSize > 0){
					switch(input.read()){
						/* Table lookup */
						case 0: {
							if(blockSize == 0x6e){
								decoded.samples = swap(input.readInt());
								decoded.length  = swap(input.readInt());
								decoded.unknown = -headerTableDec[input.read()];
								decoded.table   = new int[0x64];
								
								int ack = decoded.unknown;
								int ctr = 0;
								
								for(int i = 0; i < 0x64; i++){
									ack += headerTableDec[input.read()];
									
									decoded.table[ctr] = ack;
								}
							}
							
							break;
						}
						/* Gain */
						case 1: {
							if(blockSize > 0x10){
								decoded.gainDb = 1.0f;
								
								int value;
								
								if((value = swap(input.readInt())) != -1){
									decoded.gainDb = Float.intBitsToFloat(value);
								}
								
								if(decoded.gainDb < -40.0f){
									decoded.gainDb = 0.0f;
								}
								
								decoded.gainScale = decoded.gainDb * 0.05f;
								decoded.gainScale = (float)Math.pow(10.0, decoded.gainScale);
							}
							
							break;
						}
					}
				}
			}
		}
		
		/* Return decoded header. */
		return decoded;
	}
}

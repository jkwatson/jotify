package de.felixbruns.jotify.util;

/**
 * Class providing convenience methods for some mathematical stuff.
 * 
 * @author Felix Bruns <felixbruns@web.de>
 */
public class MathUtilities {
	/**
	 * Map a value from one range to another.
	 * 
	 * @param value The value to map.
	 * @param imin  The input range's minimum value.
	 * @param imax  The input range's maximum value.
	 * @param omin  The output range's minimum value.
	 * @param omax  The output range's maximum value.
	 * 
	 * @return The mapped value.
	 */
	public static float map(float value, float imin, float imax, float omin,
			float omax){
		return omin + (omax - omin) * ((value - imin) / (imax - imin));
	}
	
	/**
	 * Constrain a value to a range.
	 * 
	 * @param value The value to constrain
	 * @param min   The range's minimum value.
	 * @param max   The range's maximum value.
	 * 
	 * @return The constrained value.
	 */
	public static float constrain(float value, float min, float max){
		return Math.max(Math.min(value, max), min);
	}
}

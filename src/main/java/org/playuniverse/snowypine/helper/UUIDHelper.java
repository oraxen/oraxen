package org.playuniverse.snowypine.helper;

import java.security.SecureRandom;
import java.util.UUID;

import com.syntaxphoenix.syntaxapi.random.RandomNumberGenerator;

public final class UUIDHelper {

	private UUIDHelper() {
	}

	public static UUID generateUniqueId(RandomNumberGenerator generator) {
		byte[] randomBytes = new byte[16];
		System.arraycopy(longToBytes(generator.nextLong()), 0, randomBytes, 0, 8);
		System.arraycopy(longToBytes(generator.nextLong()), 0, randomBytes, 8, 8);
		randomBytes[6] &= 0x0f; /* clear version */
		randomBytes[6] |= 0x40; /* set to version 4 */
		randomBytes[8] &= 0x3f; /* clear variant */
		randomBytes[8] |= 0x80; /* set to IETF variant */
		return UUID.nameUUIDFromBytes(randomBytes);
	}

	public static UUID generateUniqueId(SecureRandom random) {
		byte[] randomBytes = new byte[16];
		random.nextBytes(randomBytes);
		randomBytes[6] &= 0x0f; /* clear version */
		randomBytes[6] |= 0x40; /* set to version 4 */
		randomBytes[8] &= 0x3f; /* clear variant */
		randomBytes[8] |= 0x80; /* set to IETF variant */
		return UUID.nameUUIDFromBytes(randomBytes);
	}

	private static byte[] longToBytes(long seed) {
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (seed & 0xFF);
			seed >>= 8;
		}
		return result;
	}

}
/**
 * 
 */
package info.guardianproject.otr.dataplug;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Copyright (C) 2013 guardian.  All rights reserved.
 *
 * @author liorsaar
 *
 */
public class Utils {
	private static String hexChr(int b) {
        return Integer.toHexString(b & 0xF);
    }

    private static String toHex(int b) {
        return hexChr((b & 0xF0) >> 4) + hexChr(b & 0x0F);
    }

    public static String sha1sum(byte[] bytes) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(bytes, 0, bytes.length);
        byte[] sha1sum = digest.digest();
        String display = "";
        for(byte b : sha1sum)
            display += toHex(b);
        return display;
    }

	public static String sha1sum(FileChannel channel) throws IOException {
		MessageDigest digest;
		try {
		    digest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
		    throw new RuntimeException(e);
		}

		ByteBuffer buffer = ByteBuffer.allocate(32768);
		while (channel.read(buffer) > 0) {
			buffer.flip();
		    digest.update(buffer);
		    buffer.clear();
		}
		
		byte[] sha1sum = digest.digest();
		String display = "";
		for(byte b : sha1sum)
		    display += toHex(b);
		return display;
	}

	public static String sanitize(String path) {
        try {
            return URLEncoder.encode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

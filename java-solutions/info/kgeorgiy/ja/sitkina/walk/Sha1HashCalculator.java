package info.kgeorgiy.ja.sitkina.walk;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1HashCalculator implements HashCalculator {
    private static final String ERROR_HASH = String.format("%040x", 0);
    private static final String SHA1_ALGO_NAME = "SHA-1";
    private static final MessageDigest digest;
    private static final int BUFFER_SIZE = 1024;
    private static final byte[] buffer = new byte[BUFFER_SIZE];

    static {
        try {
            digest = MessageDigest.getInstance(SHA1_ALGO_NAME);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String calcHash(String fileName) {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(fileName))) {
            int readBytes = stream.read(buffer);
            while (readBytes != -1) {
                digest.update(buffer, 0, readBytes);
                readBytes = stream.read(buffer);
            }
        } catch (IOException | SecurityException e) {
            return getErrorHash();
        }
        return hashToString(digest.digest());
    }

    public String getErrorHash() {
        return ERROR_HASH;
    }

    private String hashToString(byte[] hash) {
        return String.format("%0" + (hash.length << 1) + "x", new BigInteger(1, hash));
    }
}

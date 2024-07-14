package info.kgeorgiy.ja.sitkina.walk;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JenkinsHashCalculator implements HashCalculator {
    private static final String ERROR_HASH = String.format("%08x", 0);
    private static final int BUFFER_SIZE = 1024;
    private static final byte[] buffer = new byte[BUFFER_SIZE];

    public String calcHash(String fileName) {
        int hash = 0;
        try (InputStream stream = new BufferedInputStream(new FileInputStream(fileName))) {
            int readBytes = stream.read(buffer);
            while (readBytes != -1) {
                for (int i = 0; i < readBytes; i++) {
                    hash += Byte.toUnsignedInt(buffer[i]);
                    hash += hash << 10;
                    hash ^= hash >>> 6;
                }
                readBytes = stream.read(buffer);
            }
            hash += hash << 3;
            hash ^= hash >>> 11;
            hash += hash << 15;
        } catch (IOException | SecurityException e) {
            return getErrorHash();
        }
        return hashToString(hash);
    }

    public String getErrorHash() {
        return ERROR_HASH;
    }

    private String hashToString(int hash) {
        return String.format("%08x", hash);
    }
}

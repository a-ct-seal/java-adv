package info.kgeorgiy.ja.sitkina.walk;

import java.io.IOException;
import java.io.Writer;

public class HashWriter {
    private final Writer writer;
    private final HashCalculator hashCalculator;

    public HashWriter(Writer writer, HashCalculator hashCalculator) {
        this.writer = writer;
        this.hashCalculator = hashCalculator;
    }

    public void writeHash(String fileName) throws WriteException {
        try {
            writer.write(String.format("%s %s%n", hashCalculator.calcHash(fileName), fileName));
        } catch (IOException | SecurityException e) {
            throw new WriteException("Cannot write to output file", e);
        }
    }

    public void writeErrorHash(String fileName) throws WriteException {
        try {
            writer.write(String.format("%s %s%n", hashCalculator.getErrorHash(), fileName));
        } catch (IOException | SecurityException e) {
            throw new WriteException("Cannot write to output file", e);
        }
    }
}

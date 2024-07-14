package info.kgeorgiy.ja.sitkina.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashVisitor<T extends Path> extends SimpleFileVisitor<T> {

    private final HashWriter writer;

    public HashVisitor(HashWriter writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws WriteException {
        writer.writeHash(file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(T file, IOException exc) throws WriteException {
        writer.writeErrorHash(file.toString());
        return FileVisitResult.CONTINUE;
    }
}

package info.kgeorgiy.ja.sitkina.walk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class Walker {
    private static final String DEFAULT_MODE = "jenkins";

    public static void walk(String[] args, int depth, boolean log) throws IllegalInputException, WorkFilesException {
        if (args == null) {
            throw new IllegalInputException("Expected non-null args");
        }
        if (args.length != 2 && args.length != 3) {
            throw new IllegalInputException("Expected 2 or 3 args");
        }
        if (args[0] == null) {
            throw new IllegalInputException("Expected non-null input");
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalInputException("Expected non-null input");
        }
        Path inputFilePath, outputFilePath;
        try {
            inputFilePath = Path.of(args[0]);
        } catch (InvalidPathException e) {
            throw new IllegalInputException("Invalid input path: " + e.getMessage());
        }
        try {
            outputFilePath = Path.of(args[1]);
        } catch (InvalidPathException e) {
            throw new IllegalInputException("Invalid output path: " + e.getMessage());
        }
        String mode = args.length == 3 ? args[2] : DEFAULT_MODE;
        walk(inputFilePath, outputFilePath, mode, depth, log);
    }

    public static void walk(Path inputFilePath, Path outputFilePath, String mode,
                            int depth, boolean log) throws IllegalInputException, WorkFilesException {

        Path parent = outputFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException ignored) {
            }
        }
        HashCalculator hashCalculator = createHashCalculator(mode);
        try (BufferedReader reader = Files.newBufferedReader(inputFilePath, StandardCharsets.UTF_8)) {
            try (Writer writer = Files.newBufferedWriter(outputFilePath, StandardCharsets.UTF_8)) {
                HashWriter hashWriter = new HashWriter(writer, hashCalculator);
                HashVisitor<Path> visitor = new HashVisitor<>(hashWriter);
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    try {
                        Walker.makeWalk(hashWriter, line, visitor, depth, log);
                    } catch (IOException e) {
                        if (log) {
                            System.err.println("Error while handling " + line + ": " + e.getMessage());
                        }
                    }
                }
            } catch (IOException | SecurityException e) {
                throw new WorkFilesException("Writer died :(", e);
            }
        } catch (IOException | SecurityException e) {
            throw new WorkFilesException("Reader died :(", e);
        }
    }

    private static HashCalculator createHashCalculator(String mode) throws IllegalInputException {
        return switch (mode) {
            case "jenkins" -> new JenkinsHashCalculator();
            case "sha-1" -> new Sha1HashCalculator();
            default -> {
                throw new IllegalInputException("Incorrect mode " + mode);
            }
        };
    }

    private static void makeWalk(HashWriter hashWriter, String fileName,
                                 HashVisitor<Path> visitor, int depth, boolean log) throws IOException {
        Path path;
        try {
            path = Path.of(fileName);
        } catch (InvalidPathException e) {
            if (log) {
                System.err.println("Cannot parse filename " + fileName + ": " + e.getMessage());
            }
            hashWriter.writeErrorHash(fileName);
            return;
        }
        Files.walkFileTree(path, Collections.emptySet(), depth, visitor);
    }
}

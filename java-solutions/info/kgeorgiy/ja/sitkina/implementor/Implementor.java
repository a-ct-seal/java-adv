package info.kgeorgiy.ja.sitkina.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for generating implementations of classes and interfaces.
 * All generated methods return {@link #getDefaultValue(Class)} of return type.
 * Has {@link #main(String[])} to run from console.
 */
public class Implementor implements JarImpler {
    /**
     * Default Charset used for generated files and compilation.
     */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * Default constructor.
     */
    public Implementor() {
    }

    /**
     * Entry point to run from console.
     * Supported options:
     * [Canonical name {@link Class#getCanonicalName} of class to implement]
     * -jar [Canonical name {@link Class#getCanonicalName} of class to implement] [Name of result .jar file]
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        if (args == null || !(args.length == 1 || args.length == 3) || args[0] == null) {
            System.err.println("Illegal input: expected name of class to implement");
            return;
        }
        if (args.length == 3 && !args[0].equals("-jar")) {
            System.err.println("Unknown run option");
            return;
        }
        try {
            if (args.length == 3) {
                new Implementor().implementJar(resolveToken(args[1]), Path.of(args[2]));
            } else {
                new Implementor().implement(resolveToken(args[0]), Paths.get(""));
            }
        } catch (ImplerException | InvalidPathException e) {
            System.err.println("Cannot implement class: " + e.getMessage());
        }
    }

    /**
     * Resolves type token with specified name.
     * Throws {@link ImplerException} if resolve fails.
     *
     * @param tokenName name of type token.
     * @return Class Object {@link Class} representing type token with specified tokenName.
     * @throws ImplerException if token with specified tokenName cannot be found.
     */
    private static Class<?> resolveToken(String tokenName) throws ImplerException {
        try {
            return Class.forName(tokenName);
        } catch (ClassNotFoundException e) {
            throw new ImplerException("Cannot find class", e);
        }
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        writeToFile(createClass(token), token.getPackageName(), getClassName(token), root);
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        writeToJar(createClass(token), token.getPackageName(), getClassName(token), jarFile, getClassPath(token));
    }

    /**
     * Creates string with interface/class implementation.
     *
     * @param token type token of interface/class to create implementation for.
     * @return Java code of implementation.
     * @throws ImplerException if Class Object represented by this type token cannot be implemented.
     */
    private String createClass(Class<?> token) throws ImplerException {
        check(token);
        Constructor<?> constructor = getConstructor(token);
        StringBuilder stringBuilder = new StringBuilder();
        addHeader(stringBuilder, token);
        addClassDef(stringBuilder, token, constructor);
        return stringBuilder.toString();
    }

    /**
     * Checks that type can be implemented.
     * Throws {@link ImplerException} if it cannot be implemented.
     *
     * @param token type token of type to be checked.
     * @throws ImplerException if type cannot be extended/implemented.
     */
    private void check(Class<?> token) throws ImplerException {
        if (token.isPrimitive() ||
                Modifier.isFinal(token.getModifiers()) ||
                Modifier.isPrivate(token.getModifiers()) ||
                token.equals(Enum.class) ||
                token.equals(Record.class) ||
                token.isSealed()
        ) {
            throw new ImplerException("Cannot implement type");
        }
    }

    /**
     * Appends header line with package info of type to stringBuilder.
     *
     * @param stringBuilder stringBuilder to append to.
     * @param token         type token to extract package info
     */
    private void addHeader(StringBuilder stringBuilder, Class<?> token) {
        stringBuilder.append("package ").append(token.getPackageName()).append(";");
    }

    /**
     * Gets non-private constructor {@link Constructor} of specified type.
     * If type represents an interface, null is returned.
     *
     * @param token type token to get constructor from.
     * @return Constructor of type.
     * @throws ImplerException if type token represents a class with no non-private constructors.
     */
    private Constructor<?> getConstructor(Class<?> token) throws ImplerException {
        if (token.isInterface()) {
            return null;
        }
        Optional<Constructor<?>> constructor = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers())).findAny();
        if (constructor.isEmpty()) {
            throw new ImplerException("Cannot extend utility class");
        }
        return constructor.get();
    }

    /**
     * Appends class definition of generated inheritor to stringBuilder.
     * Constructor may be extracted using {@link #getConstructor(Class)}.
     *
     * @param stringBuilder stringBuilder to append class definition to.
     * @param token         type token of type to generate inheritor of.
     * @param constructor   non-private constructor of type. If null, constructor is not generated, default will be used.
     * @throws ImplerException if type cannot be extended/implemented.
     */
    private void addClassDef(StringBuilder stringBuilder, Class<?> token, Constructor<?> constructor) throws ImplerException {
        // header
        newLine(stringBuilder, 0);
        stringBuilder.append("public class ").append(getClassName(token));
        if (token.isInterface()) {
            stringBuilder.append(" implements ");
        } else {
            stringBuilder.append(" extends ");
        }
        stringBuilder.append(token.getCanonicalName()).append(" {");
        newLine(stringBuilder, 0);

        // constructor
        if (constructor != null) {
            addConstructor(stringBuilder, token, constructor);
        }

        // methods
        for (Method method : getMethodsToImplement(token)) {
            addMethod(stringBuilder, method);
        }

        // end
        newLine(stringBuilder, 0);
        stringBuilder.append("}");
    }

    /**
     * Gets all non-private methods of type and all its superclasses and superinterfaces.
     *
     * @param token type token of type to get methods from.
     * @return List of {@link Method} objects, representing found methods.
     */
    private List<Method> getMethodsFromParents(Class<?> token) {
        if (token == null) {
            return List.of();
        }
        return Stream.of(
        Arrays.stream(token.getInterfaces())
                .map(this::getMethodsFromParents)
                .map(List::stream)
                .reduce(Stream.of(), Stream::concat),
        getMethodsFromParents(token.getSuperclass()).stream(),
        Stream.of(token.getDeclaredMethods()))
                .flatMap(Function.identity())
                .filter(method -> !Modifier.isPrivate(method.getModifiers()))
                .toList();
    }

    /**
     * Gets methods of type that must be implemented in its inheritors.
     *
     * @param token type token of type to get methods from.
     * @return List of {@link Method} objects, representing found methods.
     */
    private List<Method> getMethodsToImplement(Class<?> token) {
        return getMethodsFromParents(token).stream()
                .collect(Collectors.groupingBy(Method::getName, Collectors.toList()))
                .values()
                .stream()
                .map(lst -> lst.stream()
                        .collect(Collectors.groupingBy(method -> List.of(method.getParameterTypes()), Collectors.toList()))
                        .values()
                        .stream()
                        .map(mds -> mds.stream().reduce((method_parent, method_child) -> {
                            if (!Modifier.isAbstract(method_parent.getModifiers()) ||
                                    method_parent.getReturnType().isAssignableFrom(method_child.getReturnType())) {
                                return method_child;
                            }
                            return method_parent;
                        }))
                        .map(optional -> optional.orElse(null))
                        .filter(method -> method != null && Modifier.isAbstract(method.getModifiers()))
                        .toList()
                )
                .map(Collection::stream)
                .reduce(Stream.of(), Stream::concat)
                .toList();
    }

    /**
     * Appends implementation of constructor of given type to stringBuilder.
     *
     * @param stringBuilder stringBuilder to append constructor to.
     * @param token         type token of type from which constructor was extracted.
     * @param constructor   constructor to be added.
     * @throws ImplerException if at least one of constructor parameters types has private access.
     */
    private void addConstructor(StringBuilder stringBuilder, Class<?> token,
                                Constructor<?> constructor) throws ImplerException {
        String header = String.format("%s ", getClassName(token));

        addEntity(stringBuilder, constructor.getModifiers(), header, constructor.getParameterTypes(),
                sb -> addExceptions(constructor.getExceptionTypes(), sb),
                params -> String.format("super(%s);", params));
    }

    /**
     * Appends method implementation to stringBuilder.
     *
     * @param stringBuilder stringBuilder to append method to.
     * @param method        method to be added.
     * @throws ImplerException if at least one of method parameters types or return type has private access.
     */
    private void addMethod(StringBuilder stringBuilder, Method method) throws ImplerException {
        if (Modifier.isPrivate(method.getReturnType().getModifiers())) {
            throw new ImplerException("usage of private type");
        }

        String header = String.format("%s %s", method.getReturnType().getCanonicalName(), method.getName());
        String body = String.format("return %s;", getDefaultValue(method.getReturnType()));

        addEntity(stringBuilder, method.getModifiers(), header, method.getParameterTypes(), sb -> {
        }, sb -> body);
    }

    /**
     * Appends entity (constructor or method) implementation to stringBuilder.
     *
     * @param stringBuilder   stringBuilder to append entity to.
     * @param modifiers       entity modifiers, should be extracted by {@code entity.getModifiers()}
     * @param header          start of entity declaration up to opening bracket excluding it
     * @param parameterTypes  array of entity parameter types
     * @param exceptionsAdder consumer that adds exceptions to specified stringBuilder
     * @param getBody         function that generates entity body using its parameters
     * @throws ImplerException if at least one of entity parameters types or return type has private access.
     */
    private void addEntity(StringBuilder stringBuilder, int modifiers,
                           String header, Class<?>[] parameterTypes,
                           Consumer<StringBuilder> exceptionsAdder,
                           Function<String, String> getBody) throws ImplerException {
        // header
        newLine(stringBuilder, 0);
        newLine(stringBuilder, 1);
        addModifiers(stringBuilder, modifiers);
        stringBuilder.append(header);

        // params
        stringBuilder.append("(");
        String paramsBuilder = addParams(parameterTypes, stringBuilder);
        stringBuilder.append(")");

        // exceptions
        exceptionsAdder.accept(stringBuilder);

        // body
        stringBuilder.append(" {");
        newLine(stringBuilder, 2);
        stringBuilder.append(getBody.apply(paramsBuilder));
        newLine(stringBuilder, 1);
        stringBuilder.append("}");
    }

    /**
     * Appends parameters with types and generated names to specified stringBuilder and returns parameter names separated by commas.
     * Name of parameter is "p" and its number in parameter types array.
     *
     * @param paramTypes    array of parameter types to generate function signature
     * @param stringBuilder stringBuilder to append to.
     * @return String consisting of parameter names separated by commas.
     * @throws ImplerException if at least one parameter type has private access.
     */
    private String addParams(Class<?>[] paramTypes, StringBuilder stringBuilder) throws ImplerException {
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 0; i < paramTypes.length; ++i) {
            stringBuilder.append(paramTypes[i].getCanonicalName()).append(" p").append(i);
            if (Modifier.isPrivate(paramTypes[i].getModifiers())) {
                throw new ImplerException("usage of private type");
            }
            paramsBuilder.append("p").append(i);
            if (i != paramTypes.length - 1) {
                stringBuilder.append(", ");
                paramsBuilder.append(", ");
            }
        }
        return paramsBuilder.toString();
    }

    /**
     * Appends exception types separated by commas to specified stringBuilder.
     *
     * @param exceptionTypes array of exception types to add.
     * @param stringBuilder  stringBuilder to append to.
     */
    private void addExceptions(Class<?>[] exceptionTypes, StringBuilder stringBuilder) {
        if (exceptionTypes.length != 0) {
            stringBuilder.append(" throws ");
            Arrays.stream(exceptionTypes)
                    .forEach(exceptionType -> stringBuilder.append(exceptionType.getCanonicalName()).append(", "));
            stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        }
    }

    /**
     * Appends modifiers (public/protected/private and static) to specified stringBuilder.
     *
     * @param stringBuilder stringBuilder to append to.
     * @param modifiers     entity modifiers, should be extracted by {@code entity.getModifiers()}
     */
    private void addModifiers(StringBuilder stringBuilder, int modifiers) {
        if (Modifier.isPrivate(modifiers)) {
            stringBuilder.append("private ");
        } else if (Modifier.isProtected(modifiers)) {
            stringBuilder.append("protected ");
        } else if (Modifier.isPublic(modifiers)) {
            stringBuilder.append("public ");
        }
        if (Modifier.isStatic(modifiers)) {
            stringBuilder.append("static ");
        }
    }

    /**
     * Returns default value of given type.
     * Default value for boolean is "false",
     * for void is "",
     * for all other primitive type is "0",
     * in other cases "null" is returned.
     *
     * @param clazz type token of type to get default value for.
     * @return String representation of default value for specified type.
     */
    private String getDefaultValue(Class<?> clazz) {
        if (clazz.equals(boolean.class)) {
            return "false";
        }
        if (clazz.equals(void.class)) {
            return "";
        }
        if (clazz.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Writes interface/class implementation to file.
     *
     * @param clazz       generated implementation.
     * @param packageName name of package of generated class.
     * @param className   name of generated class, may be obtained by {@link #getClassName(Class)}.
     * @param root        path to directory where to put result file.
     * @return path to directory with result file.
     * @throws ImplerException In case of problems with writing to result file.
     */
    private Path writeToFile(String clazz, String packageName, String className, Path root) throws ImplerException {
        root = root.resolve((packageName).replace(".", File.separator)).toAbsolutePath();
        if (!Files.exists(root)) {
            try {
                Files.createDirectories(root);
            } catch (IOException ignored) {
            }
        }
        root = root.resolve(className + ".java");
        try (Writer writer = Files.newBufferedWriter(root, DEFAULT_CHARSET)) {
            for (final char c : clazz.toCharArray()) {
                if ((int) c > 127) {
                    writer.write(String.format("\\u%04x", (int) c));
                } else {
                    writer.write(c);
                }
            }
        } catch (IOException e) {
            throw new ImplerException("Writer died :(", e);
        }
        return root;
    }

    /**
     * Writes interface/class implementation to <var>.jar</var> file.
     *
     * @param clazz       generated implementation.
     * @param packageName name of package of generated class.
     * @param className   name of generated class, may be obtained by {@link #getClassName(Class)}.
     * @param jarFile     path to target <var>.jar</var> file.
     * @param classPath   classpath of generated class, may be obtained by {@link #getClassPath(Class)}.
     * @throws ImplerException In case of problems with compiling code or writing to result file.
     */
    private void writeToJar(String clazz, String packageName, String className, Path jarFile, String classPath) throws ImplerException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(Path.of(""), "temp");
        } catch (IOException e) {
            throw new ImplerException("Cannot create temp dir");
        }
        Path createdClassPath = writeToFile(clazz, packageName, className, tempDir);
        List<String> toCompile = List.of(createdClassPath.toAbsolutePath().toString());
        compile(classPath, toCompile, DEFAULT_CHARSET);
        Path compiledFilePath = createdClassPath.getParent().resolve(className + ".class");
        try (JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarFile)))) {
            jarOutputStream.putNextEntry(new JarEntry(
                    packageName.replace(".", "/") + "/" + className + ".class"));
            Files.copy(compiledFilePath, jarOutputStream);
        } catch (IOException e) {
            throw new ImplerException("Cannot write to jar", e);
        }
        try {
            // :NOTE: clean from tests can be used
            deleteDirectory(tempDir);
        } catch (IOException e) {
            throw new ImplerException("Cannot delete temp dir");
        }
    }

    /**
     * Compiling java files.
     *
     * @param root    classpath of given files.
     * @param files   files to compile.
     * @param charset charset to be given to compiler.
     * @throws ImplerException If compilation fails.
     */
    private static void compile(final String root, final List<String> files, final Charset charset) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final String[] args = Stream.concat(files.stream(),
                Stream.of("-cp", root, "-encoding", charset.name())).toArray(String[]::new);
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Cannot compile your file");
        }
    }

    /**
     * Returns classpath for given type.
     *
     * @param token type token of type to get classpath of.
     * @return Classpath for given type.
     */
    private static String getClassPath(Class<?> token) {
        try {
            ProtectionDomain domain = token.getProtectionDomain();
            if (domain == null || domain.getCodeSource() == null) {
                return "";
            }
            URL url = domain.getCodeSource().getLocation();
            return Path.of(url.toURI()).toString();
        } catch (final URISyntaxException e) {
            return "";
        }
    }

    /**
     * Deletes all content of specified directory including it.
     *
     * @param dir directory to delete.
     * @throws IOException If deleting fails.
     */
    private void deleteDirectory(Path dir) throws IOException {
        try (var dirStream = Files.walk(dir)) {
            dirStream.map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        }
    }

    /**
     * Returns class name of implementation of type.
     *
     * @param token type token of type to be implemented.
     * @return Implementation simple class name.
     */
    private String getClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Appends new line and offset to specified stringBuilder.
     * Offset is a space repeated 4 * level times.
     *
     * @param stringBuilder stringBuilder to append to.
     * @param level         offset to make.
     */
    private void newLine(StringBuilder stringBuilder, int level) {
        stringBuilder.append(System.lineSeparator()).append(" ".repeat(4 * level));
    }
}

package info.kgeorgiy.ja.sitkina.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;

/**
 * Util class
 */
public class UDPUtil {
    public static final int DEFAULT_TIMEOUT = 100;
    private static final Pattern numberParser = Pattern.compile("\\d+", UNICODE_CHARACTER_CLASS);
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final int DEFAULT_BUFFER_CAPACITY = 2048;

    /** Constructs message to send from client.
     * message = prefix + threadNum + "_" + requestNum
     *
     * @param prefix message prefix
     * @param threadNum number of executing thread
     * @param requestNum number of request
     * @return message to send
     */
    public static String getMessage(final String prefix, int threadNum, int requestNum) {
        threadNum++;
        requestNum++;
        return prefix + threadNum + "_" + requestNum;
    }

    /** Checks that received data is correct: contains two numbers representing thread number and request number
     *
     * @param threadNum number of executing thread
     * @param requestNum number of request
     * @param data data to check
     * @return check result
     */
    public static boolean checkData(int threadNum, int requestNum, final String data) {
        if (data == null) {
            return false;
        }
        threadNum++;
        requestNum++;
        final long[] numbers = numberParser.matcher(data).results()
                .map(MatchResult::group).mapToLong(Long::parseLong).toArray();
        return numbers.length >= 2 && numbers[0] == threadNum && numbers[1] == requestNum;
    }

    /**
     * main to run client
     *
     * @param args main args
     */
    public static void clientMain(final String[] args, final HelloClient client) {
        if (checkArgs(args, 5)) {
            return;
        }
        final int port;
        final int threads;
        final int requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (final NumberFormatException e) {
            System.err.println("Illegal integer parameter " + e.getMessage());
            return;
        }

        client.run(args[0], port, args[2], threads, requests);
    }

    private static boolean checkArgs(final String[] args, final int argNum) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected non-null args");
            return true;
        }
        if (args.length != argNum) {
            System.err.println("Illegal number of args");
            return true;
        }
        return false;
    }

    /**
     * main to run server
     *
     * @param args main args
     */
    public static void serverMain(final String[] args, final HelloServer server) {
        if (checkArgs(args, 2)) {
            return;
        }
        final int port;
        final int threads;
        try {
            threads = Integer.parseInt(args[0]);
            port = Integer.parseInt(args[1]);
        } catch (final NumberFormatException e) {
            System.err.println("Illegal integer parameter");
            return;
        }
        server.start(threads, port);
        try (final Scanner in = new Scanner(System.in)) {
            while (true) {
                if (in.hasNext()) {
                    in.next();
                    break;
                }
            }
        }
    }

    /** Sending message
     *
     * @param message message to send
     * @param key key of channel
     * @param address address of socket to use
     * @throws IOException if channel.send thrown IOException
     */
    public static void send(final String message, final SelectionKey key, final SocketAddress address) throws IOException {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final byte[] bytes = message.getBytes(DEFAULT_CHARSET);
        channel.send(ByteBuffer.wrap(bytes), address);
        key.interestOps(SelectionKey.OP_READ);
    }

    /** Receiving message
     *
     * @param key key of channel
     * @return receiving address and received string
     * @throws IOException if channel.send thrown IOException
     */
    public static ReceiveResult receive(final SelectionKey key) throws IOException {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final ByteBuffer byteBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_CAPACITY);
        final SocketAddress address = channel.receive(byteBuffer);
        byteBuffer.flip();
        // :NOTE: non-parallel
        final byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return new ReceiveResult(address, bytes);
    }


    /** Checks if server doesn't need to start
     *
     * @param threads num of threads
     * @param ports ports and output formats
     * @return if ports.isEmpty() or threads == 0 returns true, else returns false
     */
    public static boolean noStart(final int threads, final Map<Integer, String> ports) {
        return ports.isEmpty() || threads == 0;
    }

    /** Creates answer message from data that is needed to be sent and answer format
     *
     * @param data data that is needed to be sent
     * @param ansFormat answer format
     * @return answer message
     */
    public static String getAnswer(final String data, final String ansFormat) {
        return ansFormat.replace("$", data);
    }

    /** Returns String from bytes using DEFAULT_CHARSET
     *
     * @param bytes bytes to convert to String
     * @return result String
     */
    public static String parseBytes(final byte[] bytes) {
        return new String(bytes, DEFAULT_CHARSET);
    }

    /**
     * Class containing receiving address and received string
     */
    public static class ReceiveResult {
        public final SocketAddress address;
        public final byte[] received;

        public ReceiveResult(final SocketAddress address, final byte[] received) {
            this.address = address;
            this.received = received;
        }
    }
}

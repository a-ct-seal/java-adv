package info.kgeorgiy.ja.sitkina.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements NewHelloServer {
    private ExecutorService answerService;
    private ExecutorService listenerService;
    private final List<DatagramSocket> sockets = new ArrayList<>();

    public static void main(final String[] args) {
        try (final HelloServer server = new HelloUDPServer()) {
            UDPUtil.serverMain(args, server);
        }
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        if (UDPUtil.noStart(threads, ports)) {
            return;
        }
        listenerService = Executors.newFixedThreadPool(ports.size());
        answerService = Executors.newFixedThreadPool(threads);
        for (final Map.Entry<Integer, String> entry : ports.entrySet()) {
            createListener(createSocket(entry.getKey()), entry.getValue());
        }
    }

    private DatagramSocket createSocket(final int port) {
        try {
            final DatagramSocket socket = new DatagramSocket(port);
            sockets.add(socket);
            return socket;
        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private void createListener(final DatagramSocket socket, final String ansFormat) {
        final Runnable listener = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                final DatagramPacket packet;
                try {
                    packet = new DatagramPacket(new byte[socket.getReceiveBufferSize()],
                            socket.getReceiveBufferSize());
                } catch (final SocketException e) {
                    throw new RuntimeException(e);
                }
                try {
                    socket.receive(packet);
                } catch (final IOException e) {
                    System.err.println("Receiving exception: " + e.getMessage());
                    continue;
                }
                createAnswerer(socket, packet, ansFormat);
            }
        };
        listenerService.submit(listener);
    }

    private void createAnswerer(final DatagramSocket socket, final DatagramPacket inputPacket, final String ansFormat) {
        final Runnable answerer = () -> {
            final String data = new String(inputPacket.getData(), inputPacket.getOffset(), inputPacket.getLength());
            final byte[] bytes = UDPUtil.getAnswer(data, ansFormat).getBytes(StandardCharsets.UTF_8);
            final DatagramPacket outputPacket = new DatagramPacket(bytes, bytes.length,
                    inputPacket.getAddress(), inputPacket.getPort());
            try {
                socket.send(outputPacket);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        };
        answerService.submit(answerer);
    }

    @Override
    public void close() {
        if (answerService != null && listenerService != null) {
            answerService.shutdown();
            listenerService.shutdown();
        }
        for (final DatagramSocket socket : sockets) {
            try {
                socket.close();
            } catch (final Exception e) {
                System.err.println("Cannot close socket " + e.getMessage());
            }
        }
        sockets.clear();
        if (answerService != null && listenerService != null) {
            answerService.close();
            listenerService.close();
        }
    }
}

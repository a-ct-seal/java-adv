package info.kgeorgiy.ja.sitkina.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient implements HelloClient {

    public static void main(final String[] args) {
        UDPUtil.clientMain(args, new HelloUDPClient());
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final List<DatagramSocket> sockets = new ArrayList<>();
        final ExecutorService requestService = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            try {
                final DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(UDPUtil.DEFAULT_TIMEOUT);
                sockets.add(socket);
                addThread(i, socket, host, port, prefix, requests, requestService);
            } catch (final SocketException e) {
                throw new RuntimeException(e);
            }
        }
        requestService.close();
        for (final DatagramSocket socket : sockets) {
            try {
                socket.close();
            } catch (Exception e) {
                System.err.println("Cannot close socket " + e.getMessage());
            }
        }
    }

    private void addThread(final int threadNum, final DatagramSocket socket, final String host, final int port, final String prefix,
                           final int requests, final ExecutorService requestService) {
        final Runnable task = () -> {
            for (int i = 0; i < requests; i++) {
                try {
                    String data = null;
                    final byte[] bytes = UDPUtil.getMessage(prefix, threadNum, i).getBytes(StandardCharsets.UTF_8);
                    DatagramPacket inputPacket;
                    final DatagramPacket outputPacket = new DatagramPacket(bytes, bytes.length,
                            new InetSocketAddress(host, port));
                    final byte[] buffer = new byte[socket.getReceiveBufferSize()];
                    while (!UDPUtil.checkData(threadNum, i, data)) {
                        socket.send(outputPacket);
                        inputPacket = new DatagramPacket(buffer, socket.getReceiveBufferSize());
                        try {
                            socket.receive(inputPacket);
                        } catch (final SocketTimeoutException e) {
                            continue;
                        }
                        data = new String(inputPacket.getData(), inputPacket.getOffset(),
                                inputPacket.getLength(), StandardCharsets.UTF_8);
                    }
                    System.out.println(data);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        requestService.submit(task);
    }
}

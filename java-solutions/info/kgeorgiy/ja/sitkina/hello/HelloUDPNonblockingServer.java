package info.kgeorgiy.ja.sitkina.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements NewHelloServer {
    private Selector selector;
    private ExecutorService answerService;
    private Thread mainThread;

    public static void main(final String[] args) {
        try (final HelloServer server = new HelloUDPNonblockingServer()) {
            UDPUtil.serverMain(args, server);
        }
    }

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        // :NOTE: copy-paste
        if (UDPUtil.noStart(threads, ports)) {
            return;
        }
        answerService = Executors.newFixedThreadPool(threads);
        try {
            selector = Selector.open();
            for (final Integer port : ports.keySet()) {
                final DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.bind(new InetSocketAddress(port));
                channel.register(selector, SelectionKey.OP_READ, new Attachment(ports.get(port), new ArrayDeque<>()));
            }
            mainThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        selector.select(this::performAction);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });
            mainThread.start();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        if (answerService == null) {
            return;
        }
        closeMainThread();
        closeSelector();
        answerService.close();
    }

    private void closeMainThread() {
        if (mainThread != null) {
            mainThread.interrupt();
            while (true) {
                try {
                    mainThread.join();
                    break;
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void closeSelector() {
        if (selector != null) {
            try {
                selector.keys().forEach(selectionKey -> {
                    try {
                        selectionKey.channel().close();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                selector.close();
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void performAction(final SelectionKey key) {
        try {
            final Attachment attachment = (Attachment) key.attachment();
            if (key.isReadable()) {
                final UDPUtil.ReceiveResult result = UDPUtil.receive(key);
                answerService.submit(() -> answer(key, result.received, attachment, result.address));
            }
            if (key.isWritable()) {
                // :NOTE: unsync read/write
                final DataToSend data = attachment.dataToSendQueue.poll();
                if (data == null) {
                    key.interestOps(SelectionKey.OP_READ);
                    return;
                }
                final DatagramChannel channel = (DatagramChannel) key.channel();
                channel.send(data.byteBuffer, data.address);
            }
        } catch (final IOException e) {
            // :NOTE: kill thread
            throw new UncheckedIOException(e);
        }
    }

    private void answer(
            final SelectionKey key,
            final byte[] receivedBytes,
            final Attachment attachment,
            final SocketAddress address
    ) {
        final byte[] bytes = UDPUtil.getAnswer(UDPUtil.parseBytes(receivedBytes), attachment.format).getBytes(StandardCharsets.UTF_8);
        synchronized (attachment.dataToSendQueue) {
            attachment.dataToSendQueue.add(new DataToSend(ByteBuffer.wrap(bytes), address));
            key.interestOpsOr(SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }

    private record DataToSend(ByteBuffer byteBuffer, SocketAddress address) {
    }

    private record Attachment(String format, Queue<DataToSend> dataToSendQueue) {
    }
}

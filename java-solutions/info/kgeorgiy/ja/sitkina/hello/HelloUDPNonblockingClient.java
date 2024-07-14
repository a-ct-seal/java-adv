package info.kgeorgiy.ja.sitkina.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

public class HelloUDPNonblockingClient implements HelloClient {

    public static void main(final String[] args) {
        UDPUtil.clientMain(args, new HelloUDPNonblockingClient());
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try (final Selector selector = Selector.open()) {
            final InetSocketAddress address = new InetSocketAddress(host, port);
            final List<Channel> channels = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                final DatagramChannel channel = DatagramChannel.open();
                channels.add(channel);
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, new Attachment(i, 0, requests));
            }
            while (!selector.keys().isEmpty()) {
                if (selector.select(key -> performAction(key, prefix, address), UDPUtil.DEFAULT_TIMEOUT) == 0) {
                    selector.keys().forEach(it -> it.interestOps(SelectionKey.OP_WRITE));
                }
            }
            IOException exceptions = null;
            for (final Channel channel : channels) {
                try {
                    channel.close();
                } catch (final IOException e) {
                    if (exceptions == null) {
                        exceptions = e;
                    } else {
                        exceptions.addSuppressed(e);
                    }
                }
            }
            if (exceptions != null) {
                throw new UncheckedIOException(exceptions);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void performAction(final SelectionKey key, final String prefix, final InetSocketAddress address) {
        try {
            final Attachment attachment = (Attachment) key.attachment();
            if (key.isWritable()) {
                attachment.write(key, prefix, address);
            }
            if (key.isReadable()) {
                attachment.read(key, UDPUtil.receive(key).received);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class Attachment {
        public int threadNum;
        public int requests;
        public int maxRequests;

        public Attachment(final int threadNum, final int requests, final int maxRequests) {
            this.threadNum = threadNum;
            this.requests = requests;
            this.maxRequests = maxRequests;
        }

        private void write(SelectionKey key, String prefix, InetSocketAddress address) throws IOException {
            UDPUtil.send(UDPUtil.getMessage(prefix, threadNum, requests), key, address);
        }

        private void read(SelectionKey key, byte[] received) {
            key.interestOps(SelectionKey.OP_WRITE);
            if (UDPUtil.checkData(threadNum, requests, UDPUtil.parseBytes(received))) {
                requests++;
                if (requests == maxRequests) {
                    key.cancel();
                }
            }
        }
    }
}

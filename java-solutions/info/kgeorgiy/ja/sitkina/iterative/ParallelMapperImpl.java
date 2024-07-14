package info.kgeorgiy.ja.sitkina.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Deque;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {
    private final Deque<Runnable> queue;
    private final List<Thread> threadList;

    public ParallelMapperImpl(final int threads) {
        this.queue = new ArrayDeque<>();

        final Runnable worker = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    pollRequest().run();
                }
            } catch (final InterruptedException ignored) {
            }
        };

        threadList = IntStream.range(0, threads)
                .mapToObj((i) -> new Thread(worker))
                .peek(Thread::start)
                .toList();
    }

    private Runnable pollRequest() throws InterruptedException {
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait();
            }
            queue.notify();
            return queue.pollFirst();
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        final Checker counter = new Checker();
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            final Runnable runnable = () -> {
                Exception exception = null;
                try {
                    result.set(index, f.apply(args.get(index)));
                } catch (Exception e) {
                    exception = e;
                }
                counter.increment(exception);
            };
            synchronized (queue) {
                queue.addLast(runnable);
                queue.notify();
            }
        }
        counter.check(args.size());
        return result;
    }

    @Override
    public void close() {
        threadList.forEach(Thread::interrupt);
        for (final Thread thread : threadList) {
            while (true) {
                try {
                    thread.join();
                    break;
                } catch (final InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static class Checker {
        private int count = 0;
        private Exception exception = null;

        public synchronized void increment(Exception e) {
            count++;
            if (e != null) {
                if (exception == null) {
                    exception = e;
                } else {
                    exception.addSuppressed(e);
                }
            }
            notify();
        }

        public synchronized void check(final int n) throws InterruptedException {
            while (count < n) {
                wait();
            }
            if (exception != null) {
                throw new RuntimeException(exception);
            }
        }
    }
}

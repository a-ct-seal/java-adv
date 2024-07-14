package info.kgeorgiy.ja.sitkina.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper mapper;

    public IterativeParallelism() {
        this.mapper = null;
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> T reduce(
            int threads,
            List<T> values,
            T identity,
            BinaryOperator<T> operator,
            int step
    ) throws InterruptedException {
        return applyFunction(threads, values, lst -> lst.stream().reduce(identity, operator),
                lst -> lst.stream().reduce(identity, operator), step);
    }

    @Override
    public <T, R> R mapReduce(
            int threads,
            List<T> values,
            Function<T, R> lift,
            R identity,
            BinaryOperator<R> operator,
            int step
    ) throws InterruptedException {
        return reduce(threads, map(threads, values, lift, step), identity, operator, 1);
    }

    @Override
    public String join(int threads, List<?> values, int step) throws InterruptedException {
        return applyFunction(threads, values, lst -> lst.stream().map(Object::toString).collect(Collectors.joining()),
                lst -> String.join("", lst), step);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return Collections.<T>unmodifiableList(this.<T, Stream<? extends T>>applyFunction(threads, Collections.<T>unmodifiableList(values),
                lst -> lst.stream().filter(predicate).toList().stream(),
                main_lst -> main_lst.stream().flatMap(Function.identity()), step).toList());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f, int step) throws InterruptedException {
        return Collections.<U>unmodifiableList(this.<T, Stream<? extends U>>applyFunction(threads, Collections.<T>unmodifiableList(values),
                lst -> lst.stream().map(f).toList().stream(),
                main_lst -> main_lst.stream().flatMap(Function.identity()), step).toList());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return applyFunction(threads, values, lst -> Collections.max(lst, comparator), lst -> Collections.max(lst, comparator), step);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator, int step) throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return applyFunction(threads, values, lst -> lst.stream().allMatch(predicate),
                lst -> lst.stream().allMatch(Boolean::valueOf), step);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return !all(threads, values, t -> !predicate.test(t), step);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate, int step) throws InterruptedException {
        return Math.toIntExact(applyFunction(threads, values, lst -> lst.stream().filter(predicate).count(),
                lst -> lst.stream().reduce(0L, Long::sum), step));
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return join(threads, values, 1);
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return filter(threads, values, predicate, 1);
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return map(threads, values, f, 1);
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator, 1);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return minimum(threads, values, comparator, 1);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return all(threads, values, predicate, 1);
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return any(threads, values, predicate, 1);
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return count(threads, values, predicate, 1);
    }

    private <T, S> S applyFunction(int threads, List<T> values, Function<List<T>, S> transformer,
                                   Function<List<S>, S> resultsReducer, int step) throws InterruptedException {
        if (mapper == null) {
            threads = Integer.min(threads, (values.size() + step - 1) / step);
            List<S> threadsRes = new ArrayList<>(Collections.nCopies(threads, null));
            List<Thread> threadList = new ArrayList<>();
            List<List<T>> views = cutList(threads, values, step);
            for (int i = 0; i < threads; i++) {
                int num = i;
                Thread thread = new Thread(() -> threadsRes.set(
                        num, transformer.apply(getStepsElems(views.get(num), step))));
                threadList.add(thread);
                thread.start();
            }
            joinThreads(threadList);
            return resultsReducer.apply(threadsRes);
        }
        return resultsReducer.apply(mapper.map((view) -> transformer.apply(getStepsElems(view, step)),
                cutList(threads, values, step)));
    }

    private <T> List<List<T>> cutList(int threads, List<T> values, int step) {
        List<List<T>> result = new ArrayList<>();
        int size = (values.size() + step - 1) / step;
        threads = Integer.max(Integer.min(threads, size), 1);
        int sizeForThread = size / threads;
        int incrementedBuckets = size % threads;
        int start = 0;
        for (int i = 0; i < threads; i++) {
            int end = Integer.min(start + sizeForThread * step + (i < incrementedBuckets ? step : 0), values.size());
            result.add(values.subList(start, end));
            start = end;
        }
        return result;
    }

    private void joinThreads(List<Thread> threads) throws InterruptedException {
        InterruptedException exceptions = null;
        for (int i = 0; i < threads.size(); ) {
            try {
                threads.get(i).join();
                i++;
            } catch (InterruptedException e) {
                if (exceptions == null) {
                    exceptions = e;
                } else {
                    exceptions.addSuppressed(e);
                }
            }
        }
        if (exceptions != null) {
            throw exceptions;
        }
    }

    private <T> List<T> getStepsElems(List<? extends T> values, int step) {
        if (step == 1) {
            return Collections.<T>unmodifiableList(values);
        }
        List<T> newValues = new ArrayList<>();
        for (int i = 0; i < values.size(); i += step) {
            newValues.add(values.get(i));
        }
        return newValues;
    }
}

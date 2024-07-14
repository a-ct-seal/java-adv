package info.kgeorgiy.ja.sitkina.arrayset;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SequencedCollection;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;

public class ArraySet<E> extends AbstractList<E> implements NavigableSet<E>, List<E>, SequencedCollection<E> {
    private final List<E> list;
    private final Comparator<? super E> comparator;
    // :NOTE: misleading
    private final Comparator<? super E> reversedComparatorUsedForComparableEl;

    public ArraySet() {
        this(List.of());
    }

    public ArraySet(final Collection<? extends E> c) {
        this(c, null);
    }

    public ArraySet(final Collection<? extends E> c, final Comparator<? super E> comparator) {
        this(makeList(c, comparator), comparator, Collections.reverseOrder());
    }

    private ArraySet(
            final List<E> list,
            final Comparator<? super E> comparator,
            final Comparator<? super E> reversedNaturalOrderComparator
    ) {
        this.list = list;
        this.comparator = comparator;
        this.reversedComparatorUsedForComparableEl = reversedNaturalOrderComparator;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int indexOf(final Object o) {
        final int binSearchResult = Collections.binarySearch(list, (E) o, comparator());
        if (binSearchResult >= 0) {
            return binSearchResult;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(final Object o) {
        return indexOf(o);
    }

    @Override
    public boolean contains(final Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public void addFirst(E e) {
        super.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        super.addLast(e);
    }

    @Override
    public E lower(final E e) {
        return safelyGetByIndex(lowerBound(e) - 1);
    }

    @Override
    public E floor(final E e) {
        return safelyGetByIndex(upperBound(e) - 1);
    }

    @Override
    public E ceiling(final E e) {
        return safelyGetByIndex(lowerBound(e));
    }

    @Override
    public E higher(final E e) {
        return safelyGetByIndex(upperBound(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public ArraySet<E> descendingSet() {
        return new ArraySet<>(list.reversed(), Collections.reverseOrder(comparator()),
                Collections.reverseOrder(reversedComparatorUsedForComparableEl));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        if (compareElements(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        final int end = inclusive ? upperBound(toElement) : lowerBound(toElement);
        return new ArraySet<>(list.subList(0, end), comparator(), reversedComparatorUsedForComparableEl);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        final int start = inclusive ? lowerBound(fromElement) : upperBound(fromElement);
        return new ArraySet<>(list.subList(start, size()), comparator(), reversedComparatorUsedForComparableEl);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        return list.getFirst();
    }

    @Override
    public E last() {
        return list.getLast();
    }

    @Override
    public E getFirst() {
        return list.getFirst();
    }

    @Override
    public E getLast() {
        return list.getLast();
    }

    @Override
    public E removeFirst() {
        return super.removeFirst();
    }

    @Override
    public E removeLast() {
        return super.removeLast();
    }


    @Override
    public ArraySet<E> reversed() {
        return descendingSet();
    }

    @Override
    public Spliterator<E> spliterator() {
        return NavigableSet.super.spliterator();
    }

    private int lowerBound(final E e) {
        return binarySearch(e, false);
    }

    private int upperBound(final E e) {
        return binarySearch(e, true);
    }

    private int binarySearch(final E e, final boolean isUpperBound) {
        final int binSearchResult = Collections.binarySearch(list, e, comparator());
        if (binSearchResult >= 0) {
            return binSearchResult + (isUpperBound ? 1 : 0);
        }
        return -binSearchResult - 1;
    }

    private E safelyGetByIndex(final int idx) {
        if (idx < 0 || size() <= idx) {
            return null;
        }
        return list.get(idx);
    }

    private int compareElements(final E e1, final E e2) {
        if (comparator() == null) {
            return reversedComparatorUsedForComparableEl.compare(e2, e1);
        }
        return comparator().compare(e1, e2);
    }

    private static <T> List<T> makeList(final Collection<? extends T> c, final Comparator<? super T> comparator) {
        final TreeSet<T> set = new TreeSet<>(comparator);
        set.addAll(c);
        return set.stream().toList();
    }
}

package io.th0rgal.oraxen.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class OverlayList<T> implements List<T> {

    private final List<T> originalList;

    public OverlayList(List<T> originalList) {
        this.originalList = originalList;
    }

    @Override
    public int size() {
        return originalList.size();
    }

    @Override
    public boolean isEmpty() {
        return originalList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return originalList.contains(0);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return originalList.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return originalList.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return originalList.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return originalList.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return originalList.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return originalList.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return originalList.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        return originalList.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return originalList.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return originalList.retainAll(c);
    }

    @Override
    public void clear() {
        originalList.clear();
    }

    @Override
    public T get(int index) {
        return originalList.get(index);
    }

    @Override
    public T set(int index, T element) {
        return originalList.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        originalList.add(index, element);
    }

    @Override
    public T remove(int index) {
        return originalList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return originalList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return originalList.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return originalList.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return originalList.listIterator(index);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return originalList.subList(fromIndex, toIndex);
    }

    @Override
    public void replaceAll(UnaryOperator<T> operator) {
        originalList.replaceAll(operator);
    }

    @Override
    public void sort(Comparator<? super T> c) {
        originalList.sort(c);
    }

    @Override
    public Spliterator<T> spliterator() {
        return originalList.spliterator();
    }

    @Override
    public <T1> T1[] toArray(IntFunction<T1[]> generator) {
        return originalList.toArray(generator);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return originalList.removeIf(filter);
    }

    @Override
    public Stream<T> stream() {
        return originalList.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return originalList.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        originalList.forEach(action);
    }

    public List<T> getOriginalList() {
        return originalList;
    }
}

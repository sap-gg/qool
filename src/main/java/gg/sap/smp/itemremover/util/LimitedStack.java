package gg.sap.smp.itemremover.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class LimitedStack<E> implements Iterable<E> {

    private final int size;
    private final List<E> delegate;

    public LimitedStack(final int size) {
        this.size = size;
        this.delegate = new ArrayList<>();
    }

    public void push(final E item) {
        this.delegate.add(item);
        if (this.delegate.size() > this.size) {
            this.delegate.remove(0);
        }
    }

    public int size() {
        return this.delegate.size();
    }

    public void clear() {
        this.delegate.clear();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return this.delegate.iterator();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.delegate.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return this.delegate.spliterator();
    }

}

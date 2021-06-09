package io.github.karlatemp.kasukufzos.utils;

import java.util.Enumeration;
import java.util.Iterator;

public class IteratorE<T> implements Enumeration<T> {
    private final Iterator<T> delegate;

    public IteratorE(Iterator<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasMoreElements() {
        return delegate.hasNext();
    }

    @Override
    public T nextElement() {
        return delegate.next();
    }

    public Iterator<T> asIterator() {
        return delegate;
    }
}

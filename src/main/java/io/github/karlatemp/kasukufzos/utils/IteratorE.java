/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

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

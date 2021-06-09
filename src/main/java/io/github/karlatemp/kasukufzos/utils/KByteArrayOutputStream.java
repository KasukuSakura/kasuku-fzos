/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class KByteArrayOutputStream extends ByteArrayOutputStream {
    public KByteArrayOutputStream() {
    }

    public KByteArrayOutputStream(int size) {
        super(size);
    }

    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }
}

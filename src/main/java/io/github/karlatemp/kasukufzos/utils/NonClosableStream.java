/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

import java.io.*;

public class NonClosableStream {
    public static InputStream of(InputStream is) {
        return new FilterInputStream(is) {
            @Override
            public void close() throws IOException {
            }
        };
    }

    public static OutputStream of(OutputStream os) {
        return new FilterOutputStream(os) {
            @Override
            public void close() throws IOException {
            }
        };
    }
}

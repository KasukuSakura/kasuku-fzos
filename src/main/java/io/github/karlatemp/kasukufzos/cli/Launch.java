/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.cli;

import io.github.karlatemp.kasukufzos.image.reader.KFzClassLoader;
import io.github.karlatemp.kasukufzos.image.reader.KFzReader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Launch {
    public static void main(String[] args) throws Throwable {
        KFzClassLoader.Options options = new KFzClassLoader.Options();
        options.signAction = KFzClassLoader.Options.SignAction.THROW_ON_FAILURE;
        KFzClassLoader classLoader = new KFzClassLoader(Launch.class.getClassLoader(),
                KFzReader.from(new File(args[0])), options
        );
        Class<?> aClass = classLoader.loadClass(args[1]);
        Method main = aClass.getMethod("main", String[].class);
        String[] copy = Arrays.copyOfRange(args, 2, args.length);
        try {
            main.invoke(null, (Object) copy);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}

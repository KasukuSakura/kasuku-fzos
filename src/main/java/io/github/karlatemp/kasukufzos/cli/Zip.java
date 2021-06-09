/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.cli;

import io.github.karlatemp.kasukufzos.image.writer.KFzWriter;
import io.github.karlatemp.kasukufzos.utils.TransferKit;

import java.io.File;
import java.util.Arrays;
import java.util.zip.ZipFile;

public class Zip {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage:");
            System.out.println();
            System.out.println("java ..... [output location] [library] [library] ....");
        }
        File output = new File(args[0]);
        KFzWriter writer = KFzWriter.of(output);
        for (String path : Arrays.asList(args).subList(1, args.length)) {
            emit(new File(path), writer);
        }
        writer.close();
    }

    private static void emit(File file, KFzWriter writer) throws Exception {
        if (file.isFile()) {
            System.out.println("Emitting " + file);
            try (ZipFile zf = new ZipFile(file)) {
                TransferKit.transfer(zf, file.getName(), writer);
            }
            System.out.println("Emitted  " + file);
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return;
            for (File f : files) emit(f, writer);
        }
    }
}

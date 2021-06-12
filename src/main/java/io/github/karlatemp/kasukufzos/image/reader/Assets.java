/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.image.reader;

import io.github.karlatemp.kasukufzos.utils.TransferKit;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

class Assets {
    static final X509Certificate EXPORTED_CLASSES;

    static {
        try {
            byte[] data = TransferKit.readAndClose(Assets.class.getResourceAsStream("/io/github/karlatemp/kasukufzos/exported-classes.cer"));
            EXPORTED_CLASSES = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(data));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

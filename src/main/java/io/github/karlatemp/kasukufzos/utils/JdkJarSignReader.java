/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

import org.bouncycastle.asn1.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class JdkJarSignReader {
    static boolean DEBUG = false;

    public static class Response {
        public byte[] signature;
        public ASN1ObjectIdentifier algorithmId;
        public ASN1Encodable algorithmParam;
        public ASN1ObjectIdentifier digestAlgorithmId;
        public ASN1Encodable digestAlgorithmParam;
    }

    public static void findSignature(Object data, Response response) {
        ASN1Sequence asn1Encodables = ASN1Sequence.getInstance(data);
        if (DEBUG) {
            System.out.println("TOP: " + asn1Encodables);
        }
        Iterator<ASN1Encodable> top = asn1Encodables.iterator();
        ASN1Encodable id = top.next();
        if (!KnownOIDs.JDK_SIGNED_DATA.equals(id)) {
            throw new UnsupportedOperationException("Unknown how to parse " + id);
        }
        findSignature0(ASN1Sequence.getInstance(ASN1TaggedObject.getInstance(top.next()).getObject()).iterator(), response);
    }

    private static void debug(String prefix, Object value) {
        if (DEBUG) System.out.println(prefix + value);
    }

    private static void findSignature0(Iterator<ASN1Encodable> struct, Response response) {
        struct.next(); // version
        debug("digestAlgorithmIds: ", struct.next()); // digestAlgorithmIds
        debug("contentInfo: ", struct.next()); // contentInfo
        ASN1Encodable signerInfos0 = struct.next();
        while (signerInfos0 instanceof ASN1TaggedObject) {
            debug("Dropped ", signerInfos0);
            signerInfos0 = struct.next();
        }

        for (ASN1Encodable signerInfo1 : ASN1Set.getInstance(signerInfos0)) {
            debug("Looping ", signerInfo1);

            ListIterator<ASN1Encodable> signerInfo = Arrays.asList(ASN1Sequence.getInstance(signerInfo1).toArray()).listIterator();
            signerInfo.next(); // version
            debug("issuerAndSerialNumber: ", signerInfo.next()); // issuerAndSerialNumber
            {
                ASN1Sequence digestAlgorithmId = ASN1Sequence.getInstance(signerInfo.next());
                Iterator<ASN1Encodable> iterator = digestAlgorithmId.iterator();
                response.digestAlgorithmId = ASN1ObjectIdentifier.getInstance(iterator.next());
                response.digestAlgorithmParam = iterator.next();
            }
            if (!(signerInfo.next() instanceof ASN1Set)) { // authenticatedAttributes
                signerInfo.previous();
            }
            {
                ASN1Sequence digestEncryptionAlgorithmId = ASN1Sequence.getInstance(signerInfo.next());
                Iterator<ASN1Encodable> iterator = digestEncryptionAlgorithmId.iterator();
                response.algorithmId = ASN1ObjectIdentifier.getInstance(iterator.next());
                response.algorithmParam = iterator.next();
            }
            response.signature = ASN1OctetString.getInstance(signerInfo.next()).getOctets();
            if (DEBUG) {
                while (signerInfo.hasNext()) {
                    System.out.println("Remaining: " + signerInfo.next());
                }
            }
        }
        if (DEBUG) {
            while (struct.hasNext()) {
                System.out.println("S Remaining: " + struct.next());
            }
        }
        if (response.signature != null) return;

        throw new NoSuchElementException("`encryptedDigest` not found.");
    }
}

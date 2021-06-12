/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.image.reader;

import io.github.karlatemp.kasukufzos.utils.AlgorithmId;
import io.github.karlatemp.kasukufzos.utils.JdkJarSignReader;
import io.github.karlatemp.kasukufzos.utils.ManifestDigester;
import io.github.karlatemp.kasukufzos.utils.TransferKit;

import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

class KFzClassLoaderData {
    final KFzClassLoader cl;
    final URL rootURL;

    static class KClData {
        CodeSource cs;
        URL sealBase;
        Manifest manifest;
        List<Certificate> certificates;
    }

    final KFzReaderImpl reader;

    public KFzClassLoaderData(KFzReader reader, KFzClassLoader kFzClassLoader) throws Exception {
        this.reader = (KFzReaderImpl) reader;
        this.cl = kFzClassLoader;
        rootURL = new URL("kfz", "", 0, "/", new KFzUrlStreamHandler(reader));
        initModules();
    }

    private static MessageDigest getMd(Map<String, MessageDigest> m, String t) throws Exception {
        MessageDigest digest = m.get(t);
        if (digest == null) {
            m.put(t, digest = MessageDigest.getInstance(t));
        }
        return digest;
    }

    @SuppressWarnings("DuplicatedCode")
    private void initModules() throws Exception {
        List<String> SIGN_TYPES = Arrays.asList("RSA", "DSA", "DF");
        CertificateFactory factory;
        boolean throwOnVerifyFailed = cl.options.signAction == KFzClassLoader.Options.SignAction.THROW_ON_FAILURE;
        try {
            factory = CertificateFactory.getInstance("X509");
        } catch (Exception exception) {
            if (throwOnVerifyFailed)
                throw new RuntimeException(exception);
            factory = null;
        }
        class SignInf {
            X509Certificate c;
            KFzReaderImpl.Node sf;
            String des, alg;
        }
        Map<String, MessageDigest> mds = new HashMap<>();
        for (KFzReaderImpl.Node module : reader.root.children.values()) {
            if (module.data != null) continue;
            KClData data = new KClData();
            module.data = data;

            {
                data.sealBase = new URL(rootURL, "/" + module.name + "/");
                data.cs = new CodeSource(data.sealBase, (CodeSigner[]) null);
                List<SignInf> signInfs = new ArrayList<>();
                if (cl.options.signAction != KFzClassLoader.Options.SignAction.SKIP_SIGN) {
                    for (KFzReaderImpl.Node entry : module.children.values()) {
                        if (entry.name.startsWith("META-INF/") && entry.name.endsWith(".SF")) {
                            String base = entry.name.substring(0, entry.name.length() - 2);
                            // .RSA, .DSA, .DF
                            KFzReaderImpl.Node sign = null;
                            for (String type : SIGN_TYPES) {
                                KFzReaderImpl.Node sig = module.children.get(base + type);
                                if (sig != null) {
                                    sign = sig;
                                    break;
                                }
                            }
                            if (sign == null) {
                                if (throwOnVerifyFailed) {
                                    throw new IllegalStateException(entry.name + " missing a signature file");
                                }
                                continue;
                            }
                            try {
                                byte[] signRaw = TransferKit.readAndClose(reader.resource(sign));
                                //noinspection ConstantConditions
                                CertPath pkcs7 = factory.generateCertPath(new ByteArrayInputStream(signRaw), "PKCS7");
                                // System.out.println(pkcs7);

                                X509Certificate certificate = (X509Certificate) pkcs7.getCertificates().get(pkcs7.getCertificates().size() - 1);
                                // System.out.println(certificate);
                                JdkJarSignReader.Response RSAsign = new JdkJarSignReader.Response();

                                JdkJarSignReader.findSignature(signRaw, RSAsign);
                                SignInf inf = new SignInf();
                                inf.des = AlgorithmId.find(RSAsign.digestAlgorithmId.toString()).replace("-", "");
                                inf.alg = AlgorithmId.find(RSAsign.algorithmId.toString());
                                String metx = inf.des + "with" + inf.alg;
                                Signature signature = Signature.getInstance(metx);
                                signature.initVerify(certificate);

                                try (InputStream sf = reader.resource(entry)) {
                                    TransferKit.transfer(sf, new OutputStream() {
                                        @Override
                                        public void write(int b) throws IOException {
                                            try {
                                                signature.update((byte) b);
                                            } catch (SignatureException e) {
                                                throw new IOException(e);
                                            }
                                        }

                                        @Override
                                        public void write(byte[] b, int off, int len) throws IOException {
                                            try {
                                                signature.update(b, off, len);
                                            } catch (SignatureException e) {
                                                throw new IOException(e);
                                            }
                                        }
                                    });
                                }
                                boolean result = signature.verify(RSAsign.signature);
                                if (!result) {
                                    if (throwOnVerifyFailed)
                                        throw new IOException("Illegal Signature: " + module.name + " with method " + metx + " of " + entry.name + ", " + sign.name);
                                    continue;
                                }
                                inf.c = certificate;
                                inf.sf = entry;
                                signInfs.add(inf);
                            } catch (Exception any) {
                                if (throwOnVerifyFailed) {
                                    throw any;
                                }
                            }
                        }
                    }
                    KFzReaderImpl.Node manifestEntry = module.children.get("META-INF/MANIFEST.MF");
                    if (!signInfs.isEmpty() && manifestEntry == null) {
                        if (throwOnVerifyFailed)
                            throw new IllegalStateException("Missing META-INF/MANIFEST.MF in module " + module.name);
                    }
                    byte[] manifestRaw;
                    Manifest manifest;
                    if (manifestEntry == null) {
                        manifestRaw = null;
                        manifest = null;
                    } else {
                        manifestRaw = TransferKit.readAndClose(reader.resource(manifestEntry));
                        try {
                            manifest = new Manifest(new ByteArrayInputStream(manifestRaw));
                        } catch (Exception e) {
                            if (throwOnVerifyFailed) throw e;
                            manifest = null;
                        }
                    }
                    if (throwOnVerifyFailed) {
                        assert manifest != null;
                        for (Map.Entry<String, Attributes> manifestEntryX : manifest.getEntries().entrySet()) {
                            KFzReaderImpl.Node resource = module.children.get(manifestEntryX.getKey());
                            if (resource != null) {
                                Attributes value = manifestEntryX.getValue();
                                msf:
                                {//noinspection unchecked
                                    for (Map.Entry<Attributes.Name, Object> maniEntry : (Set<Map.Entry<Attributes.Name, Object>>) (Set<?>) value.entrySet()) {
                                        if (maniEntry.getKey().toString().endsWith("-Digest")) {
                                            break msf;
                                        }
                                    }
                                    continue;
                                }
                                byte[] rs = TransferKit.readAndClose(reader.resource(resource));
                                for (Map.Entry<Attributes.Name, Object> maniEntry : TransferKit.<Map.Entry<Attributes.Name, Object>>cast(value.entrySet())) {
                                    String type = maniEntry.getKey().toString();
                                    if (type.endsWith("-Digest")) {
                                        MessageDigest digest = getMd(mds, type.substring(0, type.length() - 7));
                                        digest.reset();
                                        byte[] dg = digest.digest(rs);
                                        if (!Arrays.equals(dg, Base64.getMimeDecoder().decode(maniEntry.getValue().toString()))) {
                                            throw new IllegalStateException("MessageDigest not match: " + maniEntry.getKey());
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ManifestDigester manifestDigester = manifestRaw == null ? null : new ManifestDigester(manifestRaw);

                    infLp:
                    for (SignInf sinf : signInfs) {
                        Manifest sf;
                        try (InputStream is = resource(sinf.sf)) {
                            sf = new Manifest(is);
                        }

                        // System.out.println("Ovk " + sinf.sf.name + " " + sf.getEntries());
                        String mdm = "-Digest-Manifest";
                        if (manifestRaw != null) {
                            for (Map.Entry<Attributes.Name, Object> maniEntry : TransferKit.<Map.Entry<Attributes.Name, Object>>cast(sf.getMainAttributes().entrySet())) {
                                String t = maniEntry.getKey().toString();
                                if (t.endsWith(mdm)) {
                                    MessageDigest md = getMd(mds, t.substring(0, t.length() - mdm.length()));
                                    md.reset();
                                    if (!Arrays.equals(md.digest(manifestRaw), Base64.getMimeDecoder().decode(maniEntry.getValue().toString()))) {
                                        if (throwOnVerifyFailed) {
                                            throw new IllegalStateException("Manifest checksum failed with " + sinf.c);
                                        }
                                        continue infLp;
                                    }
                                }
                            }
                        } else continue;
                        for (Map.Entry<String, Attributes> manifestEntryX : sf.getEntries().entrySet()) {
                            ManifestDigester.Entry resource = manifestDigester.get(manifestEntryX.getKey());
                            KFzReaderImpl.Node childrenNode = module.children.get(manifestEntryX.getKey());
                            if (resource != null) {
                                Attributes value = manifestEntryX.getValue();
                                msf:
                                {//noinspection unchecked
                                    for (Map.Entry<Attributes.Name, Object> maniEntry : (Set<Map.Entry<Attributes.Name, Object>>) (Set<?>) value.entrySet()) {
                                        if (maniEntry.getKey().toString().endsWith("-Digest")) {
                                            break msf;
                                        }
                                    }
                                    continue;
                                }
                                for (Map.Entry<Attributes.Name, Object> maniEntry : TransferKit.<Map.Entry<Attributes.Name, Object>>cast(value.entrySet())) {
                                    String type = maniEntry.getKey().toString();
                                    if (type.endsWith("-Digest")) {
                                        MessageDigest md = getMd(mds, type.substring(0, type.length() - 7));
                                        md.reset();
                                        if (!Arrays.equals(resource.digest(md), Base64.getMimeDecoder().decode(maniEntry.getValue().toString()))) {
                                            throw new IllegalStateException("MessageDigest not match: " + maniEntry.getKey() + " with " + sinf.sf.name + " of " + manifestEntryX.getKey());
                                        } else {
                                            if (childrenNode != null) {
                                                KClData data1 = childrenNode.data;
                                                if (data1 == null) {
                                                    data1 = childrenNode.data = new KClData();
                                                }
                                                List<Certificate> certificates = data1.certificates;
                                                if (certificates == null) {
                                                    certificates = data1.certificates = new ArrayList<>();
                                                }
                                                certificates.add(sinf.c);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
                {
                    Collection<String> publiclyModule = cl.options.publiclyModules;
                    Certificate publiclyCertificate = cl.options.publiclyCertificate;
                    if (publiclyModule != null && publiclyModule.contains(module.name) && publiclyCertificate != null) {
                        for (KFzReaderImpl.Node children : module.children.values()) {
                            KClData kClData = children.data;
                            if (kClData == null) {
                                kClData = children.data = new KClData();
                            }
                            List<Certificate> certificates = kClData.certificates;
                            if (certificates == null) {
                                kClData.certificates = certificates = new ArrayList<>();
                            }
                            certificates.add(publiclyCertificate);
                        }
                    }

                    { // flatten
                        Map<List<Certificate>, CodeSource> csmap = new HashMap<>();
                        for (KFzReaderImpl.Node children : module.children.values()) {
                            if (children.data == null) continue;
                            List<Certificate> certificates = children.data.certificates;
                            if (certificates == null) continue;
                            children.data.certificates = null;
                            CodeSource source = csmap.get(certificates);
                            if (source != null) {
                                children.data.cs = source;
                            } else {
                                children.data.cs = new CodeSource(module.data.sealBase, certificates.toArray(new Certificate[0]));
                                csmap.put(certificates, children.data.cs);
                            }
                        }
                    }
                }
            }

            manifest:
            {
                KFzReaderImpl.Node manifest0 = module.children.get("META-INF/MANIFEST.MF");
                if (manifest0 == null) break manifest;
                Manifest manifest;
                try (InputStream res = new BufferedInputStream(reader.resource(manifest0))) {
                    manifest = new Manifest(res);
                }
                data.manifest = manifest;
            }

        }
    }

    KFzReaderImpl.Node findResource(String path) {
        for (KFzReaderImpl.Node module : reader.root.children.values()) {
            KFzReaderImpl.Node node = module.children.get(path);
            if (node != null) return node;
        }
        return null;
    }

    public InputStream resource(Object node) throws Exception {
        return reader.resource((KFzReaderImpl.Node) node);
    }

    KFzReaderImpl.Node findResAndPreDefinePackage(String path) {
        KFzReaderImpl.Node res = findResource(path);
        if (res == null) return null;
        int spl = path.lastIndexOf('/');
        if (spl == -1) return res;
        String pkgName = path.substring(0, spl).replace('/', '.');
        if (cl.pkg(pkgName) == null) {
            KClData data = res.parent.data;
            if (data == null) return res;
            Manifest manifest = data.manifest;
            if (manifest == null) return res;
            cl.definePackage0(
                    pkgName,
                    manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_TITLE),
                    manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_VERSION),
                    manifest.getMainAttributes().getValue(Attributes.Name.SPECIFICATION_VENDOR),
                    manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                    manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                    manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VENDOR),
                    data.sealBase
            );
        }
        return res;
    }
}

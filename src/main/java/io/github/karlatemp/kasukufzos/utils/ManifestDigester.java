/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

// package sun.security.util;

package io.github.karlatemp.kasukufzos.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is used to compute digests on sections of the Manifest.
 * Please note that multiple sections might have the same name, and they
 * all belong to a single Entry.
 */
public class ManifestDigester {

    /**
     * The part "{@code Manifest-Main-Attributes}" of the main attributes
     * digest header name in a signature file as described in the jar
     * specification:
     * <blockquote>{@code x-Digest-Manifest-Main-Attributes}
     * (where x is the standard name of a {@link MessageDigest} algorithm):
     * The value of this attribute is the digest value of the main attributes
     * of the manifest.</blockquote>
     *
     * @see <a href="{@docRoot}/../specs/jar/jar.html#signature-file">
     * JAR File Specification, section Signature File</a>
     * @see #getMainAttsEntry
     */
    public static final String MF_MAIN_ATTRS = "Manifest-Main-Attributes";

    /**
     * the raw bytes of the manifest
     */
    private final byte[] rawBytes;

    private final ManifestDigester.Entry mainAttsEntry;

    /**
     * individual sections by their names
     */
    private final HashMap<String, ManifestDigester.Entry> entries = new HashMap<>();

    /**
     * state returned by findSection
     */
    static class Position {
        int endOfFirstLine; // not including newline character

        int endOfSection; // end of section, not including the blank line
        // between sections
        int startOfNext;  // the start of the next section
    }

    /**
     * find a section in the manifest.
     *
     * @param offset should point to the starting offset with in the
     *               raw bytes of the next section.
     * @return false if end of bytes has been reached, otherwise returns
     * true
     * @pos set by
     */
    @SuppressWarnings("fallthrough")
    private boolean findSection(int offset, ManifestDigester.Position pos) {
        int i = offset, len = rawBytes.length;
        int last = offset - 1;
        int next;
        boolean allBlank = true;

        /* denotes that a position is not yet assigned.
         * As a primitive type int it cannot be null
         * and -1 would be confused with (i - 1) when i == 0 */
        final int UNASSIGNED = Integer.MIN_VALUE;

        pos.endOfFirstLine = UNASSIGNED;

        while (i < len) {
            byte b = rawBytes[i];
            switch (b) {
                case '\r':
                    if (pos.endOfFirstLine == UNASSIGNED)
                        pos.endOfFirstLine = i - 1;
                    if (i < len - 1 && rawBytes[i + 1] == '\n')
                        i++;
                    /* fall through */
                case '\n':
                    if (pos.endOfFirstLine == UNASSIGNED)
                        pos.endOfFirstLine = i - 1;
                    if (allBlank || (i == len - 1)) {
                        pos.endOfSection = allBlank ? last : i;
                        pos.startOfNext = i + 1;
                        return true;
                    } else {
                        // start of a new line
                        last = i;
                        allBlank = true;
                    }
                    break;
                default:
                    allBlank = false;
                    break;
            }
            i++;
        }
        return false;
    }

    public ManifestDigester(byte[] bytes) {
        rawBytes = bytes;

        ManifestDigester.Position pos = new ManifestDigester.Position();

        if (!findSection(0, pos)) {
            mainAttsEntry = null;
            return; // XXX: exception?
        }

        // create an entry for main attributes
        mainAttsEntry = new ManifestDigester.Entry().addSection(new ManifestDigester.Section(
                0, pos.endOfSection + 1, pos.startOfNext, rawBytes));

        int start = pos.startOfNext;
        while (findSection(start, pos)) {
            int len = pos.endOfFirstLine - start + 1;
            int sectionLen = pos.endOfSection - start + 1;
            int sectionLenWithBlank = pos.startOfNext - start;

            if (len >= 6) { // 6 == "Name: ".length()
                if (isNameAttr(bytes, start)) {
                    KByteArrayOutputStream nameBuf = new KByteArrayOutputStream();
                    nameBuf.write(bytes, start + 6, len - 6);

                    int i = start + len;
                    if ((i - start) < sectionLen) {
                        if (bytes[i] == '\r'
                                && i + 1 - start < sectionLen
                                && bytes[i + 1] == '\n') {
                            i += 2;
                        } else {
                            i += 1;
                        }
                    }

                    while ((i - start) < sectionLen) {
                        if (bytes[i++] == ' ') {
                            // name is wrapped
                            int wrapStart = i;
                            while (((i - start) < sectionLen)
                                    && (bytes[i] != '\r')
                                    && (bytes[i] != '\n')) i++;
                            int wrapLen = i - wrapStart;
                            if (i - start < sectionLen) {
                                i++;
                                if (bytes[i - 1] == '\r'
                                        && i - start < sectionLen
                                        && bytes[i] == '\n')
                                    i++;
                            }

                            nameBuf.write(bytes, wrapStart, wrapLen);
                        } else {
                            break;
                        }
                    }

                    entries.computeIfAbsent(nameBuf.toString(UTF_8),
                            dummy -> new ManifestDigester.Entry())
                            .addSection(new ManifestDigester.Section(start, sectionLen,
                                    sectionLenWithBlank, rawBytes));
                }
            }
            start = pos.startOfNext;
        }
    }

    private boolean isNameAttr(byte[] bytes, int start) {
        return ((bytes[start] == 'N') || (bytes[start] == 'n')) &&
                ((bytes[start + 1] == 'a') || (bytes[start + 1] == 'A')) &&
                ((bytes[start + 2] == 'm') || (bytes[start + 2] == 'M')) &&
                ((bytes[start + 3] == 'e') || (bytes[start + 3] == 'E')) &&
                (bytes[start + 4] == ':') &&
                (bytes[start + 5] == ' ');
    }

    public static class Entry {

        // One Entry for one name, and one name can have multiple sections.
        // According to the JAR File Specification: "If there are multiple
        // individual sections for the same file entry, the attributes in
        // these sections are merged."
        private List<ManifestDigester.Section> sections = new ArrayList<>();
        boolean oldStyle;

        private ManifestDigester.Entry addSection(ManifestDigester.Section sec) {
            sections.add(sec);
            return this;
        }

        /**
         * Check if the sections (particularly the last one of usually only one)
         * are properly delimited with a trailing blank line so that another
         * section can be correctly appended and return {@code true} or return
         * {@code false} to indicate that reproduction is not advised and should
         * be carried out with a clean "normalized" newly-written manifest.
         *
         * @see #reproduceRaw
         */
        public boolean isProperlyDelimited() {
            return sections.stream().allMatch(
                    ManifestDigester.Section::isProperlySectionDelimited);
        }

        public void reproduceRaw(OutputStream out) throws IOException {
            for (ManifestDigester.Section sec : sections) {
                out.write(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
            }
        }

        public byte[] digest(MessageDigest md) {
            md.reset();
            for (ManifestDigester.Section sec : sections) {
                if (oldStyle) {
                    ManifestDigester.Section.doOldStyle(md, sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                } else {
                    md.update(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                }
            }
            return md.digest();
        }

        /**
         * Netscape doesn't include the new line. Intel and JavaSoft do
         */

        public byte[] digestWorkaround(MessageDigest md) {
            md.reset();
            for (ManifestDigester.Section sec : sections) {
                md.update(sec.rawBytes, sec.offset, sec.length);
            }
            return md.digest();
        }
    }

    private static class Section {
        int offset;
        int length;
        int lengthWithBlankLine;
        byte[] rawBytes;

        public Section(int offset, int length,
                       int lengthWithBlankLine, byte[] rawBytes) {
            this.offset = offset;
            this.length = length;
            this.lengthWithBlankLine = lengthWithBlankLine;
            this.rawBytes = rawBytes;
        }

        /**
         * Returns {@code true} if the raw section is terminated with a blank
         * line so that another section can possibly be appended resulting in a
         * valid manifest and {@code false} otherwise.
         */
        private boolean isProperlySectionDelimited() {
            return lengthWithBlankLine > length;
        }

        private static void doOldStyle(MessageDigest md,
                                       byte[] bytes,
                                       int offset,
                                       int length) {
            // this is too gross to even document, but here goes
            // the 1.1 jar verification code ignored spaces at the
            // end of lines when calculating digests, so that is
            // what this code does. It only gets called if we
            // are parsing a 1.1 signed signature file
            int i = offset;
            int start = offset;
            int max = offset + length;
            int prev = -1;
            while (i < max) {
                if ((bytes[i] == '\r') && (prev == ' ')) {
                    md.update(bytes, start, i - start - 1);
                    start = i;
                }
                prev = bytes[i];
                i++;
            }
            md.update(bytes, start, i - start);
        }
    }

    /**
     * @see #MF_MAIN_ATTRS
     */
    public ManifestDigester.Entry getMainAttsEntry() {
        return mainAttsEntry;
    }

    /**
     * @see #MF_MAIN_ATTRS
     */
    public ManifestDigester.Entry getMainAttsEntry(boolean oldStyle) {
        mainAttsEntry.oldStyle = oldStyle;
        return mainAttsEntry;
    }

    public ManifestDigester.Entry get(String name) {
        return entries.get(name);
    }

    public ManifestDigester.Entry get(String name, boolean oldStyle) {
        ManifestDigester.Entry e = get(name);
        if (e == null && MF_MAIN_ATTRS.equals(name)) {
            e = getMainAttsEntry();
        }
        if (e != null) {
            e.oldStyle = oldStyle;
        }
        return e;
    }

    public byte[] manifestDigest(MessageDigest md) {
        md.reset();
        md.update(rawBytes, 0, rawBytes.length);
        return md.digest();
    }

}

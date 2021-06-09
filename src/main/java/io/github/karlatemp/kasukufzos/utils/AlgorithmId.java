/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

public class AlgorithmId {
    public static String find(String algorithmId) {
        SunKnownOIDs match = SunKnownOIDs.findMatch(algorithmId);
        if (match != null) {
            return match.stdName();
        }
        return algorithmId;
    }
}

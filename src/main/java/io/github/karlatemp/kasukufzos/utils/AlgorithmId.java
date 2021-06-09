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

package com.haisa.terminal.emulatorview.compat;

public class AndroidCharacterCompat {
    public static final int EAST_ASIAN_WIDTH_NEUTRAL = 0;
    public static final int EAST_ASIAN_WIDTH_AMBIGUOUS = 1;
    public static final int EAST_ASIAN_WIDTH_HALF_WIDTH = 2;
    public static final int EAST_ASIAN_WIDTH_FULL_WIDTH = 3;
    public static final int EAST_ASIAN_WIDTH_NARROW = 4;
    public static final int EAST_ASIAN_WIDTH_WIDE = 5;

    public static int getEastAsianWidth(char c) {
        int type = Character.getType(c);
        switch (type) {
            case Character.OTHER_LETTER:
            case Character.MODIFIER_LETTER:
                return EAST_ASIAN_WIDTH_WIDE;
            case Character.COMBINING_SPACING_MARK:
            case Character.NON_SPACING_MARK:
            case Character.ENCLOSING_MARK:
                return EAST_ASIAN_WIDTH_NARROW;
            default:
                break;
        }
        if (isCJKCodePoint(c)) {
            return EAST_ASIAN_WIDTH_WIDE;
        }
        if (isHalfWidthCJK(c)) {
            return EAST_ASIAN_WIDTH_HALF_WIDTH;
        }
        if (isAmbiguousWidthCJK(c)) {
            return EAST_ASIAN_WIDTH_AMBIGUOUS;
        }
        return EAST_ASIAN_WIDTH_NARROW;
    }

    private static boolean isCJKCodePoint(char c) {
        return (c >= '\u2E80' && c <= '\u2EFF') ||
               (c >= '\u3000' && c <= '\u303F') ||
               (c >= '\u3040' && c <= '\u33FF') ||
               (c >= '\u3400' && c <= '\u4DBF') ||
               (c >= '\u4E00' && c <= '\u9FFF') ||
               (c >= '\uA000' && c <= '\uA4CF') ||
               (c >= '\uAC00' && c <= '\uD7AF') ||
               (c >= '\uF900' && c <= '\uFAFF') ||
               (c >= '\uFE30' && c <= '\uFE6F') ||
               (c >= '\uFF01' && c <= '\uFF60') ||
               (c >= '\uFFE0' && c <= '\uFFE6');
    }

    private static boolean isHalfWidthCJK(char c) {
        return (c >= '\uFF61' && c <= '\uFF9F');
    }

    private static boolean isAmbiguousWidthCJK(char c) {
        return (c >= '\u00A1' && c <= '\u00A1') ||
               (c >= '\u00B4' && c <= '\u00B4') ||
               (c >= '\u00D7' && c <= '\u00D7') ||
               (c >= '\u00F7' && c <= '\u00F7') ||
               (c >= '\u2010' && c <= '\u2010') ||
               (c >= '\u2013' && c <= '\u2016') ||
               (c >= '\u2018' && c <= '\u2019') ||
               (c >= '\u201C' && c <= '\u201D') ||
               (c >= '\u2020' && c <= '\u2022') ||
               (c >= '\u2024' && c <= '\u2027') ||
               (c >= '\u2030' && c <= '\u2030') ||
               (c >= '\u2032' && c <= '\u2033') ||
               (c >= '\u2035' && c <= '\u2035') ||
               (c >= '\u203B' && c <= '\u203B') ||
               (c >= '\u203E' && c <= '\u203E') ||
               (c >= '\u2074' && c <= '\u2074') ||
               (c >= '\u207F' && c <= '\u207F') ||
               (c >= '\u2081' && c <= '\u2084') ||
               (c >= '\u20AC' && c <= '\u20AC') ||
               (c >= '\u2103' && c <= '\u2103') ||
               (c >= '\u2105' && c <= '\u2105') ||
               (c >= '\u2109' && c <= '\u2109') ||
               (c >= '\u2113' && c <= '\u2113') ||
               (c >= '\u2116' && c <= '\u2116') ||
               (c >= '\u2121' && c <= '\u2122') ||
               (c >= '\u2126' && c <= '\u2126') ||
               (c >= '\u212B' && c <= '\u212B') ||
               (c >= '\u2153' && c <= '\u2154') ||
               (c >= '\u215B' && c <= '\u215E') ||
               (c >= '\u2160' && c <= '\u216B') ||
               (c >= '\u2170' && c <= '\u2179') ||
               (c >= '\u2190' && c <= '\u2199') ||
               (c >= '\u21B8' && c <= '\u21B9') ||
               (c >= '\u21D2' && c <= '\u21D2') ||
               (c >= '\u21D4' && c <= '\u21D4') ||
               (c >= '\u21E7' && c <= '\u21E7') ||
               (c >= '\u2200' && c <= '\u2200') ||
               (c >= '\u2202' && c <= '\u2203') ||
               (c >= '\u2207' && c <= '\u2208') ||
               (c >= '\u220B' && c <= '\u220B') ||
               (c >= '\u220F' && c <= '\u220F') ||
               (c >= '\u2211' && c <= '\u2211') ||
               (c >= '\u2215' && c <= '\u2215') ||
               (c >= '\u221A' && c <= '\u221A') ||
               (c >= '\u221D' && c <= '\u2220') ||
               (c >= '\u2223' && c <= '\u2223') ||
               (c >= '\u2225' && c <= '\u2225') ||
               (c >= '\u2227' && c <= '\u222C') ||
               (c >= '\u222E' && c <= '\u222E') ||
               (c >= '\u2234' && c <= '\u2237') ||
               (c >= '\u223C' && c <= '\u223D') ||
               (c >= '\u2248' && c <= '\u2248') ||
               (c >= '\u224C' && c <= '\u224C') ||
               (c >= '\u2252' && c <= '\u2252') ||
               (c >= '\u2260' && c <= '\u2261') ||
               (c >= '\u2264' && c <= '\u2267') ||
               (c >= '\u226A' && c <= '\u226B') ||
               (c >= '\u226E' && c <= '\u226F') ||
               (c >= '\u2282' && c <= '\u2283') ||
               (c >= '\u2286' && c <= '\u2287') ||
               (c >= '\u2295' && c <= '\u2295') ||
               (c >= '\u2299' && c <= '\u2299') ||
               (c >= '\u22A5' && c <= '\u22A5') ||
               (c >= '\u22BF' && c <= '\u22BF') ||
               (c >= '\u2312' && c <= '\u2312') ||
               (c >= '\u2460' && c <= '\u24E9') ||
               (c >= '\u2500' && c <= '\u254B') ||
               (c >= '\u2550' && c <= '\u2573') ||
               (c >= '\u2580' && c <= '\u258F') ||
               (c >= '\u2592' && c <= '\u2595') ||
               (c >= '\u25A0' && c <= '\u25A1') ||
               (c >= '\u25A3' && c <= '\u25A9') ||
               (c >= '\u25B2' && c <= '\u25B3') ||
               (c >= '\u25B6' && c <= '\u25B7') ||
               (c >= '\u25BC' && c <= '\u25BD') ||
               (c >= '\u25C0' && c <= '\u25C1') ||
               (c >= '\u25C6' && c <= '\u25C8') ||
               (c >= '\u25CB' && c <= '\u25CB') ||
               (c >= '\u25CE' && c <= '\u25D1') ||
               (c >= '\u25E2' && c <= '\u25E5') ||
               (c >= '\u25EF' && c <= '\u25EF') ||
               (c >= '\u2605' && c <= '\u2606') ||
               (c >= '\u2609' && c <= '\u2609') ||
               (c >= '\u260E' && c <= '\u260F') ||
               (c >= '\u2614' && c <= '\u2615') ||
               (c >= '\u261C' && c <= '\u261C') ||
               (c >= '\u261E' && c <= '\u261E') ||
               (c >= '\u2640' && c <= '\u2640') ||
               (c >= '\u2642' && c <= '\u2642') ||
               (c >= '\u2660' && c <= '\u2661') ||
               (c >= '\u2663' && c <= '\u2665') ||
               (c >= '\u2667' && c <= '\u266A') ||
               (c >= '\u266C' && c <= '\u266D') ||
               (c >= '\u266F' && c <= '\u266F') ||
               (c >= '\u273D' && c <= '\u273D') ||
               (c >= '\u2776' && c <= '\u277F') ||
               (c >= '\uE000' && c <= '\uF8FF') ||
               (c >= '\uFE00' && c <= '\uFE0F') ||
               (c >= '\uFFFD' && c <= '\uFFFD');
    }
}

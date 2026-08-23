package com.campus.xianyu.common;

import java.util.Locale;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

public final class SearchText {
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = createFormat();

    private SearchText() {
    }

    public static boolean matches(String keyword, String... candidates) {
        String query = normalize(keyword);
        if (query.isEmpty()) {
            return true;
        }
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String normalized = normalize(candidate);
            if (normalized.contains(query)) {
                return true;
            }
            String fullPinyin = fullPinyin(candidate);
            if (fullPinyin.contains(query) || pinyinInitials(candidate).contains(query)) {
                return true;
            }
        }
        return false;
    }

    public static int score(String keyword, String candidate, int fieldWeight) {
        String query = normalize(keyword);
        if (query.isEmpty() || candidate == null || candidate.isBlank()) {
            return 0;
        }
        String normalized = normalize(candidate);
        if (normalized.equals(query)) {
            return fieldWeight + 30;
        }
        if (normalized.contains(query)) {
            return fieldWeight + 20;
        }
        if (fullPinyin(candidate).contains(query) || pinyinInitials(candidate).contains(query)) {
            return fieldWeight + 10;
        }
        return 0;
    }

    private static String fullPinyin(String value) {
        try {
            return normalize(PinyinHelper.toHanYuPinyinString(value, PINYIN_FORMAT, "", true));
        } catch (Exception exception) {
            return "";
        }
    }

    private static String pinyinInitials(String value) {
        StringBuilder initials = new StringBuilder();
        for (char character : value.toCharArray()) {
            try {
                String[] values = PinyinHelper.toHanyuPinyinStringArray(character, PINYIN_FORMAT);
                if (values != null && values.length > 0 && !values[0].isBlank()) {
                    initials.append(values[0].charAt(0));
                } else if (Character.isLetterOrDigit(character)) {
                    initials.append(Character.toLowerCase(character));
                }
            } catch (Exception exception) {
                if (Character.isLetterOrDigit(character)) {
                    initials.append(Character.toLowerCase(character));
                }
            }
        }
        return initials.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private static HanyuPinyinOutputFormat createFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        return format;
    }
}

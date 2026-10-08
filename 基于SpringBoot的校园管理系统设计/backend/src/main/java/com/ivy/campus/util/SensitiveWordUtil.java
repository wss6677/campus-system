package com.ivy.campus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感词工具：静态词库，支持由 sys_sensitive_word 表刷新
 */
public final class SensitiveWordUtil {

    private static final String MASK = "*";

    /** 阻断级词库，命中即拒绝 */
    private static volatile Set<String> BLOCK_WORDS = new LinkedHashSet<>();

    /** 警告级词库，命中仅提示 */
    private static volatile Set<String> WARN_WORDS = new LinkedHashSet<>();

    /** 词 -> 替换文本 */
    private static final Map<String, String> REPLACEMENT = new ConcurrentHashMap<>();

    /** 缓存的匹配模式 */
    private static volatile Pattern CACHED_PATTERN;

    static {
        refresh(Arrays.asList("暴力", "赌博", "诈骗", "涉黄"), Arrays.asList("谣言", "违规", "举报不实"));
    }

    private SensitiveWordUtil() {
    }

    /** 由数据库词库刷新静态词库 */
    public static void refresh(Collection<String> blockWords, Collection<String> warnWords) {
        BLOCK_WORDS = normalize(blockWords);
        WARN_WORDS = normalize(warnWords);
        REPLACEMENT.clear();
        CACHED_PATTERN = null;
    }

    /** 设置某个词的替换文本 */
    public static void putReplacement(String word, String replacement) {
        if (word != null && !word.isBlank()) {
            REPLACEMENT.put(word, replacement == null ? MASK : replacement);
        }
    }

    /** 返回命中的敏感词，未命中返回空集合 */
    public static List<String> hit(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        Pattern pattern = pattern();
        if (pattern == null) {
            return Collections.emptyList();
        }
        Matcher matcher = pattern.matcher(text);
        List<String> hits = new ArrayList<>();
        while (matcher.find()) {
            String word = matcher.group();
            if (!hits.contains(word)) {
                hits.add(word);
            }
        }
        return hits;
    }

    /** 是否命中阻断级敏感词 */
    public static boolean hasBlock(String text) {
        return hit(text).stream().anyMatch(word -> BLOCK_WORDS.contains(word));
    }

    /** 是否命中警告级敏感词 */
    public static boolean hasWarn(String text) {
        return hit(text).stream().anyMatch(word -> WARN_WORDS.contains(word));
    }

    /** 将命中的敏感词替换为掩码 */
    public static String replace(String text) {
        List<String> hits = hit(text);
        if (hits.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : hits) {
            String replacement = REPLACEMENT.getOrDefault(word, repeatMask(word.length()));
            result = result.replace(word, replacement);
        }
        return result;
    }

    /** 当前静态词库数量 */
    public static int size() {
        return pattern() == null ? 0 : BLOCK_WORDS.size() + WARN_WORDS.size();
    }

    private static synchronized Pattern pattern() {
        if (CACHED_PATTERN != null) {
            return CACHED_PATTERN;
        }
        Set<String> all = new LinkedHashSet<>(BLOCK_WORDS);
        all.addAll(WARN_WORDS);
        if (all.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String word : all) {
            if (word == null || word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(Pattern.quote(word));
        }
        if (builder.length() == 0) {
            return null;
        }
        CACHED_PATTERN = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
        return CACHED_PATTERN;
    }

    private static Set<String> normalize(Collection<String> words) {
        Set<String> result = new LinkedHashSet<>();
        if (words != null) {
            for (String word : words) {
                if (word != null && !word.isBlank()) {
                    result.add(word.trim());
                }
            }
        }
        return result;
    }

    private static String repeatMask(int length) {
        return MASK.repeat(Math.max(length, 1));
    }
}

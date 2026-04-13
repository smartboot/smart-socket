package org.smartboot.socket.transport;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frish2021
 * 用于格式化banner以及提供格式化banner时的占位符API
 */
final class BannerFormat {
    private final static Pattern pattern =  Pattern.compile("\\$\\{([^}]+)}");
    private final Map<String, Format> map = new WeakHashMap<>();
    private final IoServerConfig config;

    BannerFormat(IoServerConfig config) {
        this.config = config;

        {
            map.put("version", ignore -> IoServerConfig.VERSION);
            map.put("port", IoServerConfig::getPort);
            map.put("threads", IoServerConfig::getThreadNum);
            map.put("backlog", IoServerConfig::getBacklog);
        }
    }

    void printf(String line) {
        Matcher matcher = pattern.matcher(line);
        StringBuffer result = new StringBuffer();
        replaceAll(matcher, (key) -> containsKeyReplace(key, (replacement) -> {
            matcher.appendReplacement(result, replacement);
        }));

        matcher.appendTail(result);
        System.out.println(result);
    }

    /**
     * 替换展位符的同时判断是否是存在的占位符
     */
    private void containsKeyReplace(String key, Replace replace) {
        if (map.containsKey(key)) {
            String replacement = toString(key);
            replace.replace(replacement);
        }
    }

    /**
     * 替换所有的占位符
     */
    private void replaceAll(Matcher matcher, Replace replace) {
        while (matcher.find()) {
            String key = matcher.group(1);
            replace.replace(key);
        }
    }

    /**
     * 获取到展位符的值
     */
    private String toString(String key) {
        return map.get(key).format(config).toString();
    }

    /**
     * 用于replaceAll和containsKeyReplace方法的封装
     */
    @FunctionalInterface
    interface Replace {
        void replace(String key);
    }

    /**
     * 占位符格式化接口
     */
    @FunctionalInterface
    interface Format {
        Object format(IoServerConfig config);
    }
}

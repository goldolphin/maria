package net.goldolphin.maria.common;

/**
 * @author caofuxiang
 *         2014-03-19 15:35
 */
public class UrlUtils {
    private static final char DELIMITER = '/';

    /**
     * Concat several url paths into one.
     * @param paths
     * @return
     */
    public static String concat(String ... paths) {
        StringBuilder buffer = new StringBuilder();
        char last = Character.MIN_VALUE;
        for (String path: paths) {
            if (last != DELIMITER && buffer.length() > 0) {
                buffer.append(DELIMITER);
                last = DELIMITER;
            }

            int len = path.length();
            for (int i = 0; i < len; i ++) {
                char c = path.charAt(i);
                if (c != DELIMITER || last != DELIMITER) {
                    buffer.append(c);
                }
                last = c;
            }
        }

        return buffer.toString();
    }
}

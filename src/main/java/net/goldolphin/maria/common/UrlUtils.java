package net.goldolphin.maria.common;

/**
 * @author caofuxiang
 *         2014-03-19 15:35
 */
public class UrlUtils {
    private static final String DELIMITER = "/";

    /**
     * Concat several url paths into one.
     * @param parent
     * @param parts
     * @return
     */
    public static String concat(String parent, String ... parts) {
        StringBuilder builder = new StringBuilder(parent);
        for (String part: parts) {
            joinPaths(builder, part);
        }
        return builder.toString();
    }

    public static String getParent(String path) {
        int i = path.lastIndexOf(DELIMITER);
        return path.substring(0, i);
    }

    public static String getBasename(String path) {
        int i = path.lastIndexOf(DELIMITER);
        return path.substring(i + 1);
    }

    private static void joinPaths(StringBuilder parent, String child) {
        if (child == null || child.length() == 0) {
            return;
        }
        if (!endsWith(parent, DELIMITER)) {
            parent.append(DELIMITER);
        }
        if (child.startsWith(DELIMITER)) {
            parent.append(child.substring(DELIMITER.length()));
        } else {
            parent.append(child);
        }
    }

    private static boolean endsWith(StringBuilder builder, String prefix) {
        int begin = builder.length() - prefix.length();
        if (begin < 0) {
            return false;
        }
        for (int i = 0; i < prefix.length(); ++i) {
            if (builder.charAt(begin + i) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}

package net.goldolphin.maria.pattern;

/**
 * Match urls with user-defined patterns, and retrieve corresponding user-defined data.
 * @author caofuxiang
 *         2014-03-19 12:57
 */
public class UrlMatcher<T> {
    private final State<T> startState = new State<T>();

    private UrlMatcher() {
    }

    /**
     * Add pattern and corresponding user-defined data, which will be returned when matching successfully.
     * @param pattern url-like string, such as /device/$deviceId/[$resource]/[user]. A segment starting with '$' is a
     *                named matching variable/parameter, which can be replaced with arbitrary valid segment. A single '$'
     *                represents a unnamed matching.
     *                Any segments can be wrapped with '[' and ']' to represent optional.
     * @param data user-defined data corresponding to this pattern.
     */
    public void addPattern(String pattern, T data) {
        String[] splits = pattern.split("/");
        State<T> current = startState;
        for (String s: splits) {
            boolean optional = false;
            if (s.startsWith("[") && s.endsWith("]")) {
                s = s.substring(1, s.length()-1);
                optional = true;
            }

            if (s.startsWith("$")) {
                current = current.addNontermTransition(s.substring(1), optional);
            } else {
                current = current.addTermTransition(s, optional);
            }
        }
        current.accept(data);
    }

    /**
     * Try to match a whole urlPath.
     * @param urlPath
     * @return
     */
    public MatchResult<T> match(String urlPath) {
        String[] splits = urlPath.split("/");
        MatchResult<T> result = new MatchResult<T>();
        startState.match(splits, 0, result);
        return result;
    }

    /**
     * Construct a new {@link UrlMatcher}
     * @param <T>
     * @return
     */
    public static <T> UrlMatcher<T> newInstance() {
        return new UrlMatcher<T>();
    }
}

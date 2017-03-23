package net.goldolphin.maria.pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of {@link UrlMatcher#match}
 * @author caofuxiang
 *         2014-03-19 13:41
 */
public class MatchResult<T> {
    private boolean isMatched = false;
    private T data;
    private final HashMap<String, String> parameters = new HashMap<String, String>();

    void matched(boolean isMatched, T data) {
        this.isMatched = isMatched;
        this.data = data;
    }

    /**
     * Whether the matching complete successfully.
     * @return
     */
    public boolean isMatched() {
        return isMatched;
    }

    /**
     * Return user-defined data corresponding to the matched pattern.
     * @return
     */
    public T getData() {
        return data;
    }

    /**
     * Return the set of all named parameters defined in the matched pattern.
     * @return
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "MatchResult{" +
                "isMatched=" + isMatched +
                ", data=" + data +
                ", parameters=" + parameters +
                '}';
    }
}

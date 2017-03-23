package net.goldolphin.maria.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author caofuxiang
 *         2014-03-19 11:24
 */

public class State<T> {
    private final HashMap<String, State<T>> termTransitions = new HashMap<String, State<T>>();
    private final HashMap<String, State<T>> nontermTransitions = new HashMap<String, State<T>>();
    private final ArrayList<State<T>> optionalTransitions = new ArrayList<State<T>>();

    private boolean isAccepted = false;
    private T data = null;

    public State<T> addTermTransition(String term, boolean optional) {
        State<T> next = termTransitions.get(term);
        if (next == null) {
            next = new State<T>();
            termTransitions.put(term, next);
        }

        if (optional) {
            optionalTransitions.add(next);
        }
        return next;
    }

    public State<T> addNontermTransition(String nonterm, boolean optional) {
        State<T> next = nontermTransitions.get(nonterm);
        if (next == null) {
            next = new State<T>();
            nontermTransitions.put(nonterm, next);
        }

        if (optional) {
            optionalTransitions.add(next);
        }
        return next;
    }

    public void accept(T data) {
        if (isAccepted) {
            throw new IllegalStateException("State cannot accept more than one pattern!");
        }
        this.data = data;
        isAccepted = true;
    }

    public boolean match(String[] input, int i, MatchResult<T> result) {
        if (i < input.length) {
            String current = input[i];
            State<T> next = termTransitions.get(current);
            if (next != null) {
                if (next.match(input, i + 1, result)) {
                    return true;
                }
            }

            for (Map.Entry<String, State<T>> t: nontermTransitions.entrySet()) {
                if (t.getValue().match(input, i + 1, result)) {
                    String nonterm = t.getKey();
                    if (nonterm.length() > 0) {
                        result.getParameters().put(t.getKey(), current);
                    }
                    return true;
                }
            }
        }

        for (State<T> t: optionalTransitions) {
            if (t.match(input, i, result)) {
                return true;
            }
        }

        if (i == input.length && isAccepted) {
            result.matched(true, data);
            return true;
        }

        return false;
    }
}

package com.intuit.karate.matching;

import java.util.LinkedHashMap;
import java.util.Map;

import com.intuit.karate.Match;
import com.intuit.karate.MatchOperation;
import com.intuit.karate.Match.Type;

public class Failures {
    private final Match.Type type;

    private final Map<String, IMatchingOperation> pathToOperation = new LinkedHashMap<>();
    
    public Failures(Type type) {
        this.type = type;
    }

    /** 
     * Adds a new failure reason for the specified context.
     * 
     * If a reason already exists for the context, both will be appended.
     * 
     * A context is uniquely identified by its path. 
     * expected and actual are in fact redundant (a couple expected/actual is meant to be uniquer per path), but they allow for more descriptive error message.
     */
    public void addFailure(MatchingOperation operation) {
        pathToOperation.put(operation.context.path(), operation);
    }

    public String getFailureReasons() {
        return MatchOperation.collectFailureReasons(pathToOperation.values(), type);
    }
}

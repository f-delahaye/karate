package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;

// TODO add assertion to make sure only ONE method is called, or else we may end up with NOT NOT.
public class NotOperator implements MatchingOperator {

    private final MatchingOperator delegate;
    
    public NotOperator(MatchingOperator delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matchArray(List<?> expected, Object actual, FailureCollector collector, Context context) {
        return !delegate.matchArray(expected, actual, collector, context);
    }

    @Override
    public boolean matchObject(Map<String, ?> expected, Object actual, FailureCollector collector, Context context) {
        return !delegate.matchObject(expected, actual, collector, context);
    }

    @Override
    public boolean matchLiteral(Object expected, Object actual, FailureCollector collector, Context context) {
        return !delegate.matchLiteral(expected, actual, collector, context);
    }

}

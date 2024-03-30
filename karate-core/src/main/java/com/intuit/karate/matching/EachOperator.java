package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;

public class EachOperator implements MatchingOperator {

    private final MatchingOperator operator;

    public EachOperator(MatchingOperator operator) {
        this.operator = operator;
    }

    @Override
    public boolean matchArray(List<?> expected, Object actual, FailureCollector collector, Context context) {
        return doMatch(expected, actual, collector, context);
    }

    @Override
    public boolean matchObject(Map<String, ?> expected, Object actual, FailureCollector collector, Context context) {
        return doMatch(expected, actual, collector, context);        
    }

    @Override
    public boolean matchLiteral(Object expected, Object actual, FailureCollector collector, Context context) {
        return doMatch(expected, actual, collector, context);
    }

    private boolean doMatch(Object expected, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof List<?> actualList) {
            for (Object actualItem: actualList) {
                if (!new MatchHandler().matches(expected, operator, actualItem, collector, context)) {
                    return false;
                }
            }
            return true;
        }
        return false;        
    }

}

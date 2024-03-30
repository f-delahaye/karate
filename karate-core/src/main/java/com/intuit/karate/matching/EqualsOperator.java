package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;


public class EqualsOperator implements ConcreteOperator {
    public static MatchingOperator EQUALS = new EqualsOperator();
    public static MatchingOperator NOT_EQUALS = new NotOperator(EQUALS);
    public static MatchingOperator EACH_EQUALS = new EachOperator(EQUALS);
    public static MatchingOperator EACH_NOT_EQUALS = new EachOperator(NOT_EQUALS);

    
    @Override
    public boolean matchArray(List<?> expectedList, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof List<?> actualList) {
            return equals(expectedList, actualList, collector, context);
        }
        return false;
    }

    private boolean equals(List<?> expectedList, List<?> actualList, FailureCollector collector, Context context) {
        if (expectedList.size() != actualList.size()) {
            return false;
        }
        for (int i=0; i<expectedList.size(); i++) {
            if (!new MatchHandler().matches(expectedList.get(i), nestedOperator(), actualList.get(i), collector, context)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean matchObject(Map<String, ?> expectedObject, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof Map actualObject) {
            return expectedObject.size() == actualObject.size() && ContainsOperator.contains(expectedObject, actualObject, nestedOperator(), collector, context);
        }
        return false;
    }

    @Override
    public MatchingOperator nestedOperator() {
            // Nested comparisons are always performed in terms of equals.
        return this;
    }

}

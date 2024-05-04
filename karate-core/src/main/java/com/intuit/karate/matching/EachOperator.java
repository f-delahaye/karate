package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.Match.Context;

public class EachOperator implements MatchingOperator {

    private final MatchingOperator operator;

    public EachOperator(MatchingOperator operator) {
        this.operator = operator;
    }

    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {
        for (int i=0; i<actualList.size(); i++) {
            Object actual = actualList.get(i);
            Context context = operation.context;
            context.putJS("_$", actual);
            if (!MatchingOperation.of(actual, expected, context.descend(i), operator, operation.failures).execute()) {
                return operation.fail("actual item "+i+ " doesnot match expected");
            }
            context.removeJS("_$");
        }
        return true;
    }


    @Override
    public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        // #2516
        for (Map.Entry<String, ?> actualEntry: actualObject.entrySet()) {
            String actualKey = actualEntry.getKey();
            if (! MatchingOperation.of(actualEntry.getValue(), expected, operation.context.descend(actualKey), operator, operation.failures).execute()) {
                return operation.fail("actual item "+actualKey+ " doesnot match expected");
            }
        }
        return true;
    }

    @Override
    public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        return operation.fail("actual is not a list or a map");
    }

    @Override
    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        if (actual instanceof List<?> actualList) {
            return matchList(actualList, macro, operation);
        }
        return operation.fail("actual is not a list or a map");
    }
}

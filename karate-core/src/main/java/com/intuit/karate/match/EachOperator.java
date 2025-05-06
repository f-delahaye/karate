package com.intuit.karate.match;

import com.intuit.karate.match.Match.Context;

import java.util.List;
import java.util.Map;

public class EachOperator implements Operator {

    private final boolean matchEachEmptyAllowed;
    private final Operator operator;

    public EachOperator(boolean matchEachEmptyAllowed, Operator operator) {
        this.matchEachEmptyAllowed = matchEachEmptyAllowed;
        this.operator = operator;
    }

    private boolean executeList(MatchOperation operation) {
        List<?> actualList = operation.actual.getValue();
        if (actualList.isEmpty() && !matchEachEmptyAllowed) {
            return operation.fail("match each failed, empty array / list");
        }
        for (int i=0; i<actualList.size(); i++) {
            Object actual = actualList.get(i);
            Context context = operation.context;
            context.JS.put("_$", actual);
            if (!operator.execute(new MatchOperation(context.descend(i), new Match.Value(actual), operation.expected))) {
                return operation.fail("match each failed at index "+i);
            }
            context.JS.bindings.removeMember("_$");
        }
        return true;
    }


    private boolean executeObject(MatchOperation operation) {
        Map<String, ?> actualObject = operation.actual.getValue();
        if (actualObject.isEmpty() && !matchEachEmptyAllowed) {
            return operation.fail("match each failed, empty object");
        }
        // #2516
        for (Map.Entry<String, ?> actualEntry: actualObject.entrySet()) {
            String actualKey = actualEntry.getKey();
            if (! operator.execute(new MatchOperation(operation.context.descend(actualKey), new Match.Value(actualEntry.getValue()), operation.expected))) {
                return operation.fail("actual item "+actualKey+ " does not match expected");
            }
        }
        return true;
    }

    @Override
    public boolean execute(MatchOperation operation) {
        Match.Value actual = operation.actual;
        if (actual.isList()) {
            return executeList(operation);
        }
        if (actual.isMap() || actual.isXml()) {
            return executeObject(operation);
        }
        return operation.fail("actual is not an array or list");
    }
}

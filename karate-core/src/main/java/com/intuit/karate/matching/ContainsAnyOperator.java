package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

public class ContainsAnyOperator extends ConcreteOperator {
    
    public static final MatchingOperator CONTAINS_ANY = new ContainsAnyOperator(false);
    
    public static final MatchingOperator NOT_CONTAINS_ANY = new NotOperator(CONTAINS_ANY);
    
    public static final MatchingOperator EACH_CONTAINS_ANY = new EachOperator(CONTAINS_ANY);
    
    public static final MatchingOperator CONTAINS_ANY_DEEP = new ContainsAnyOperator(true);
    
    public static final MatchingOperator NOT_CONTAINS_ANY_DEEP = new NotOperator(CONTAINS_ANY_DEEP);
    
    public ContainsAnyOperator(boolean deep) {
        super(deep);
    }

    @Override
    public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        return EqualsOperator.EQUALS.matchLiteral(actualLiteral, expected, operation);
    }

    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {
        if (expected instanceof List<?> expectedList) {
            for (Object expectedItem: expectedList) {
                if (ContainsOperator.actualListContainsItem(actualList, expectedItem, operation, nestedOperator())) {
                    return true;
                }
            }
        } else if (ContainsOperator.actualListContainsItem(actualList, expected, operation, nestedOperator())) {
            return true;
        }
        return operation.fail("actual array does not contain any of the expected items");
    }

    @Override
    public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        if (expected instanceof Map<?, ?> expectedObject) {
            for (Map.Entry<?, ?>  expectedEntry: expectedObject.entrySet()) {
                String expectedProperty = (String) expectedEntry.getKey();
                Object actual = actualObject.get(expectedProperty);
                if (actual != null && MatchingOperation.of(actual, expectedEntry.getValue(), operation.context.descend(expectedProperty), nestedOperator(), operation.failures).execute()) {
                    return true;
                }
            }
            return operation.fail("actual object does not contain any of the expected items");
        }
        return operation.fail("data types don't match");
    }

    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        // per doc, macro forces equals. Not sure if we should throw unsupportedOperation, or delegate to equals.
        return EqualsOperator.EQUALS.matchMacro(actual, macro, operation);
    }    
}

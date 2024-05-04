package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

public class ContainsOnlyOperator extends ConcreteOperator {

    public static MatchingOperator CONTAINS_ONLY = new ContainsOnlyOperator(false);
    public static MatchingOperator NOT_CONTAINS_ONLY = new NotOperator(CONTAINS_ONLY);
    public static MatchingOperator CONTAINS_ONLY_DEEP = new ContainsOnlyOperator(true);
    public static MatchingOperator NOT_CONTAINS_ONLY_DEEP = new NotOperator(CONTAINS_ONLY_DEEP);
    public static MatchingOperator EACH_CONTAINS_ONLY = new EachOperator(CONTAINS_ONLY);
    public static MatchingOperator EACH_NOT_CONTAINS_ONLY = new EachOperator(NOT_CONTAINS_ONLY);


	public ContainsOnlyOperator(boolean deep) {
		super(deep);
	}

    @Override
    public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        return EqualsOperator.EQUALS.matchLiteral(actualLiteral, expected, operation);
    }

    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {
        if (expected instanceof List expectedList) {
            return expectedList.size() == actualList.size() &&ContainsOperator.actualListContainsExpectedList(actualList, expectedList, operation, nestedOperator(), "not equals");
        }
        if (!ContainsOperator.actualListContainsItem(actualList, expected, operation, nestedOperator())) {
            return operation.fail("actual does not match expected");
        }
        return true;
    }

    @Override
    public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        if (expected instanceof Map expectedObject) {
            return expectedObject.size() == actualObject.size() &&ContainsOperator.actualObjectContainsExpectedObject(actualObject, expectedObject, operation, nestedOperator(), "not equals");
        }
        return operation.fail("data types don't match");
    }

    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        // per doc, macro forces equals. Not sure if we should throw unsupportedOperation, or delegate to equals.
        return EqualsOperator.EQUALS.matchMacro(actual, macro, operation);
    }    
}


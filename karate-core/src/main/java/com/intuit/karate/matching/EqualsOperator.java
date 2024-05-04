package com.intuit.karate.matching;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.intuit.karate.MatchOperation;


public class EqualsOperator extends ConcreteOperator {
    public static MatchingOperator EQUALS = new EqualsOperator(false);
    public static MatchingOperator NOT_EQUALS = new NotOperator(EQUALS);
    public static MatchingOperator EACH_EQUALS = new EachOperator(EQUALS);
    public static MatchingOperator EACH_NOT_EQUALS = new EachOperator(NOT_EQUALS);

	public EqualsOperator(boolean deep) {
		super(deep);
	}    
    
    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {
        if (expected instanceof List<?> expectedList) {            
            if (expectedList.size() != actualList.size()) {
                return operation.fail("actual array length is not equal to expected - "+actualList.size()+":"+expectedList.size());
            }
            for (int i=0; i<expectedList.size(); i++) {
                Object expectedChild = expectedList.get(i);
                Object actualChild = actualList.get(i);
                if (!MatchingOperation.of(actualChild, expectedChild, operation.context.descend(i), nestedOperator(), operation.failures).execute()) {
                    return operation.fail("not equals | array match failed at index "+i);
                }
            }
            return true;
        }
        return operation.fail("data types don't match");
    }

    @Override
    public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        if (expected instanceof Map expectedObject) {
            if (!expectedObject.keySet().containsAll(actualObject.keySet())) {
            // if (expectedObject.size() < actualObject.size()) {
                // Size based comparison is not enough, because expected may contain optional properties that will pass
                // but will skew any size comparison 
                return operation.fail("actual has "+(actualObject.size() - expectedObject.size())+" more key(s) than expected");
            } 
            // All actual keys are contained in expected keys.
            // NOw validate the other way around ie all expected entries are contained in actual entries 
            return ContainsOperator.actualObjectContainsExpectedObject(actualObject, expectedObject, operation, nestedOperator(), "not equals");
        }
        return operation.fail("data types don't match");
    }

    @Override
    public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        if (actualLiteral == null || expected == null) {
            return matchOrFail(actualLiteral == expected, operation);
        }
        if (actualLiteral instanceof Number actualNumber) {
            if (expected instanceof Number expectedNumber) {
                return matchOrFail(numberEquals(actualNumber, expectedNumber), operation);
            }
        } else if (actualLiteral instanceof byte[] actualBytes) {
            if (expected instanceof byte[] expectedBytes) {
                return matchOrFail(Arrays.equals(actualBytes, expectedBytes), operation);
            }
        } else if (actualLiteral.getClass() == expected.getClass()) {
            return matchOrFail(actualLiteral.equals(expected), operation);
        }
        return operation.fail("data types don't match");
    }

    private static boolean numberEquals(Number actualNumber, Number expectedNumber) {
        if (actualNumber instanceof BigDecimal || expectedNumber instanceof BigDecimal) {
            return MatchOperation.toBigDecimal(actualNumber).compareTo(MatchOperation.toBigDecimal(expectedNumber)) == 0;
        } 
        return actualNumber.doubleValue() == expectedNumber.doubleValue();

    }

    private static boolean matchOrFail(boolean match, MatchingOperation operation) {
        if (match) {
            return true;
        }
        return operation.fail("not equals");
    }

    @Override
    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        Boolean macroResult = MatchOperation.macroEqualsExpected(macro, operation, operation::ofLegacy);
        if (macroResult != null) {
            return macroResult;
            // null means not handled and typically happens when expected is a regular string (not a macro) which happens to start with #.
            // in that case, we want it to be handled by matchLiteral
        }
        return operation.executeNonMacro();
    }
}

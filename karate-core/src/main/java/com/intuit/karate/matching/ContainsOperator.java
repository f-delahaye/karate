package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

/**
 * Operator for match contains.
 * 
 * This class is final by designed.
 * It is tempting to make e.g. ContainsOnly extend Contains. But some methods' implementation are specific to contain and not suitable for the other contain* operators,  
 * 
 * Instead, when logic is application to multiple contain operators, it should be exposed as a static method.
 */
public final class ContainsOperator extends ConcreteOperator {

    public static final MatchingOperator CONTAINS = new ContainsOperator(false);
    public static final MatchingOperator CONTAINS_DEEP = new ContainsOperator(true);
    public static final MatchingOperator NOT_CONTAINS = new NotOperator(CONTAINS);
    public static final MatchingOperator NOT_CONTAINS_DEEP = new NotOperator(CONTAINS_DEEP);
    public static final MatchingOperator EACH_CONTAINS = new EachOperator(CONTAINS);
    public static final MatchingOperator EACH_NOT_CONTAINS = new EachOperator(NOT_CONTAINS);
    public static final MatchingOperator EACH_CONTAINS_DEEP = new EachOperator(CONTAINS_DEEP);

	public ContainsOperator(boolean deep) {
		super(deep);
	}

    /**
     * match actual contains <literal>
     * 
     * returns true iff:
     * * actual is an array that contains <literal>
     * * actual is a literal equals to <literal>
     *   
     */
	@Override
	public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        if (actualLiteral instanceof String actualStr) {
            if (expected instanceof CharSequence expectedStr) {
                if (actualStr.contains(expectedStr)) {
                    return true;
                }               
                return operation.fail("actual does not contain expected");
            } 
             // String contains Integer for example (although that particular case could be implemented, actually)
            return operation.fail("data types don't match");
        }
        return EqualsOperator.EQUALS.matchLiteral(actualLiteral, expected, operation);
	}

    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {        
        if (expected instanceof List<?> expectedList) {
            return actualListContainsExpectedList(actualList, expectedList, operation, nestedOperator(), "not contains");
        }
        
        // else expected is a single item, checking whether it is contained in actualList
        for (int i=0; i<actualList.size();i++) {
            Object actual = actualList.get(i);
            if (MatchingOperation.of(actual, expected, operation.context.descend(i), nestedOperator(), operation.failures).execute()) {
                return true;
            }
        } 

        return operation.fail("actual does not contain item");
    }

    static boolean actualListContainsExpectedList(List<?> actualList, List<?> expectedList, MatchingOperation operation, MatchingOperator operator, String failureMessage) {
        for (int i=0; i<expectedList.size(); i++) {
            Object expectedItem = expectedList.get(i);
            if (!actualListContainsItem(actualList, expectedItem, operation, operator)) {
                return operation.fail(failureMessage + " | actual does not match expected");
            }
        }
        return true;

    }


    /** Returns true if any element of actualList matches (per the provided operator) the provided expected  */
    static boolean actualListContainsItem(List<?> actualList, Object expectedItem, MatchingOperation operation, MatchingOperator operator) {
        for (int i=0;i<actualList.size();i++) {
            Object actual = actualList.get(i);
            MatchingOperation childOperation = MatchingOperation.of(actual, expectedItem, operation.context.descend(i), operator, operation.failures);
            if (childOperation.execute()) {
                return true;
            }
        }
        return false;
    }

	@Override
	public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        if (expected instanceof Map expectedObject) {
            return actualObjectContainsExpectedObject(actualObject, expectedObject, operation, nestedOperator(), "not contains");
        }
        return operation.fail("types don't match");
	}

    static boolean actualObjectContainsExpectedObject(Map<String, ?> actualObject, Map<String, ?> expectedObject, MatchingOperation operation, MatchingOperator operator, String failureReason) {
        for (Map.Entry<String, ?> expected: expectedObject.entrySet()) {
            String expectedKey = expected.getKey();
            Object expectedValue = expected.getValue();
            // If key is not present, do not fail if it is optional.
            // However, if it is present, do validate it, even if it's optional.
            // Note that "key not present", from a Karate perspective, is not exactly the same as "value is null".
            // The former is handled here, the latter will be in Match.VALIDATORS.
            // See js-arrays.feature lines 253 vs 269            
            if (!actualObject.containsKey(expectedKey)) {
                if (isOptionalKey(expectedValue)) {
                    return true;
                }
                return operation.fail("actual does not contain key "+expectedKey);
            }
            MatchingOperation childOperation = MatchingOperation.of(actualObject.get(expectedKey), expectedValue, operation.context.descend(expected.getKey()), operator, operation.failures);
            if (!childOperation.execute()) {
                return operation.fail(failureReason + " | match failed for name: " +expectedKey);
            }            
        }
        return true;
    }

    static boolean isOptionalKey(Object value) {
        return value instanceof String valueStr && (valueStr.startsWith("##") || valueStr.equals("#ignore") || valueStr.equals("#notpresent"));
    }

    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        // per doc, macro forces equals. Not sure if we should throw unsupportedOperation, or delegate to equals.
        return EqualsOperator.EQUALS.matchMacro(actual, macro, operation);
    }
 }

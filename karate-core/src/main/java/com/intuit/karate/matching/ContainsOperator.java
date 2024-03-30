package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;

/**
 * Operator for match contains.
 * 
 */
public class ContainsOperator implements ConcreteOperator {

    public static final MatchingOperator CONTAINS = new ContainsOperator(false);
    public static final MatchingOperator CONTAINS_DEEP = new ContainsOperator(true);
    public static final MatchingOperator NOT_CONTAINS = new NotOperator(CONTAINS);
    public static final MatchingOperator NOT_CONTAINS_DEEP = new NotOperator(CONTAINS_DEEP);
    public static final MatchingOperator EACH_CONTAINS = new EachOperator(CONTAINS);
    public static final MatchingOperator EACH_NOT_CONTAINS = new EachOperator(NOT_CONTAINS);
    public static final MatchingOperator EACH_CONTAINS_DEEP = new EachOperator(CONTAINS_DEEP);

    final boolean deep;

	public ContainsOperator(boolean deep) {
		this.deep = deep;
	}

	@Override
	public MatchingOperator nestedOperator() {
        return deep?this:EqualsOperator.EQUALS;
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
	public boolean matchLiteral(Object expected, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof Iterable actualIterable) {
            return contains(actualIterable, expected, nestedOperator(), collector, context);
        } else {
            return ConcreteOperator.super.matchLiteral(expected, actual, collector, context);
        }
	}

    @Override
    public boolean matchArray(List<?> expectedList, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof List<?> actualList) {
            return matchArrays(expectedList, actualList, collector, context);
        } 
        return false;
    }

    protected boolean matchArrays(List<?> expectedList, List<?> actualList, FailureCollector collector, Context context) {
        MatchingOperator operator = nestedOperator();        
        for (Object expected: expectedList) {
            if (!contains(actualList, expected, operator, collector, context)) {
                return false;
            }
        }
        return true;

    }

    /** Returns true if any element of actualList matches (per the provided operator) the provided expected  */
    private boolean contains(Iterable<?> actualList, Object expected, MatchingOperator operator, FailureCollector collector, Context context) {
        for (Object actual: actualList) {
            if (new MatchHandler().matches(expected, operator, actual, collector, context)) {
                return true;
            }
        }
        return false;
    }


	@Override
	public boolean matchObject(Map<String, ?> expectedObject, Object actual, FailureCollector collector, Context context) {
        if (actual instanceof Map actualObject) {
            return matchObjects(expectedObject, actualObject, collector, context);
        }
        return false;
	}

    protected boolean matchObjects(Map<String, ?> expectedObject, Map<String, ?> actualObject, FailureCollector collector, Context context) {
        return contains(expectedObject, actualObject, nestedOperator(), collector, context);        
    }

    protected static boolean contains(Map<String, ?> expectedObject, Map<String, ?> actualObject, MatchingOperator operator, FailureCollector collector, Context context) {
        for (Map.Entry<String, ?> expected: expectedObject.entrySet()) {
            Object expectedValue = expected.getValue();
            Object actualValue = actualObject.get(expected.getKey());
            if (actualValue == null || !new MatchHandler().matches(expectedValue, operator, actualValue, collector, context)) {
                return false;
            }
        }
        return true;
    }


    

}

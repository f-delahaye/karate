package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;

public class ContainsOnlyOperator extends ContainsOperator {

    public static MatchingOperator CONTAINS_ONLY = new ContainsOnlyOperator(false);
    public static MatchingOperator NOT_CONTAINS_ONLY = new NotOperator(CONTAINS_ONLY);
    public static MatchingOperator CONTAINS_ONLY_DEEP = new ContainsOnlyOperator(true);
    public static MatchingOperator EACH_CONTAINS_ONLY = new EachOperator(CONTAINS_ONLY);
    public static MatchingOperator EACH_NOT_CONTAINS_ONLY = new EachOperator(NOT_CONTAINS_ONLY);


	public ContainsOnlyOperator(boolean deep) {
		super(deep);
	}

    @Override
    protected boolean matchArrays(List<?> expectedList, List<?> actualList, FailureCollector collector, Context context) {
        return expectedList.size() == actualList.size() &&super.matchArrays(expectedList, actualList, collector, context);
    }

    @Override
    protected boolean matchObjects(Map<String, ?> expectedObject, Map<String, ?> actualObject, FailureCollector collector, Context context) {
        return expectedObject.size() == actualObject.size() &&super.matchObjects(expectedObject, actualObject, collector, context);
    }

}


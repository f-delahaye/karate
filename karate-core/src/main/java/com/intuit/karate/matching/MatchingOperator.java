package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.Match.Context;

public interface MatchingOperator {

    /** Default implementation, suitable when both arguments are literals.
     * expected, by definition, will always be a literal but actual could be another datatype.
     * In those cases, specific operators may want to override this method.
     * 
     * For example, if actual is an array and the operator is contains, this method may be overridden to return true if the array contains the expected literal.
     * @param expected
     * @param actual
     * @param collector
     * @param context TODO
     * @return
    */
    default boolean matchLiteral(Object expected, Object actual, FailureCollector collector, Context context) {
        // TODO: refine. We should handle BigDecimal vs Number comparisons the same way as MatchOperation
        return expected.equals(actual);
    }

    boolean matchArray(List<?> expected, Object actual, FailureCollector collector, Context context);

    boolean matchObject(Map<String, ?> expected, Object actual, FailureCollector collector, Context context);
}

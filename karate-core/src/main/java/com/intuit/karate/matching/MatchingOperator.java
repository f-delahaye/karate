package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

public interface MatchingOperator {

    boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation);

    boolean matchList(List<?> actualList, Object expected, MatchingOperation operation);

    boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation);

    boolean matchMacro(Object actual, String macro, MatchingOperation operation);
}

package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;

import com.intuit.karate.Match;
import com.intuit.karate.XmlUtils;

/**
 *  Does not inherit from MatchOperation because the new world works with simple objects and operators, while the old one works with Match.Value and Match.Type.
 *  In essence however, {@link MatchingOperation} follows most of the same principles and implements the same {@link IMatchingOperation}
 * - its execute method acts as a router and delegates to the appropriate method of operator.
 * - in addition, it may be flagged as failed
 * - and it contains all the details to describe a step failure.
 * 
 * Also note that this class does sometimes use Match.Value for actual. But these usages are inherited from the old world (Match.Operation parameters) that might be refactored / removed at some point. Internally; it does (or could) use regular objects.
 *  
 */
public class MatchingOperation implements IMatchingOperation {

 
    private final Match.Value actual;
    private final Match.Value expected;
    public final Match.Context context;
    public final MatchingOperator operator;
    public final Failures failures;

    private String failReason = null;

    public MatchingOperation(Object actual, Object expected, Match.Context context, MatchingOperator operator, Failures failures) {
        this.actual = new Match.Value(actual);
        this.expected = new Match.Value(expected);
        this.context = context;
        this.operator = operator;
        this.failures = failures;
    }

    @Override
    public Match.Value expected() {
        return expected;
    }


    @Override
    public Match.Value actual() {
        return actual;
    }

    @Override
    public Match.Context context() {
        return context;
    }

    @Override 
    public String failReason() {
        return failReason;
    }

    @Override
    public boolean execute() {
        Object expected = this.expected.getValue();
        Object actual = this.actual.getValue();
        if (expected instanceof String expectedStr && expectedStr.startsWith("#")) {
            // Boolean macroResult = MatchOperation.macroEqualsExpected(expectedStr, this, this::ofLegacy);
            // if (macroResult != null) {
            //     return macroResult;
            //     // else continue. null means not handled and typically happens when expected is a regular string (not a macro) which happens to start with #.
            //     // in that case, we want it to be handled by matchLiteral
            // }
             
            return operator.matchMacro(actual, expectedStr, this);            
        }

        return executeNonMacro();
    }

    // Ideally, this would be part of execute(), however, it is necessary to handle processMacro.
    // An alternative solution might be to move execute directly in MatchingOperator, but not sure if it's a good idea.
    boolean executeNonMacro() {
        Object expected = this.expected.getValue();
        Object actual = this.actual.getValue();

        if (actual instanceof List<?> actualList) {
            return operator.matchList(actualList, expected, this);
        }
        if (actual instanceof Node actualNode) {
            actual = (Map<String, ?>) XmlUtils.toObject(actualNode, true);
        }        
        if (actual instanceof Map actualObject) {
            if (expected instanceof Node expectedNode) {
                expected = (Map<String, ?>) XmlUtils.toObject(expectedNode, true);
            }
            return operator.matchObject(actualObject, expected, this);
        }
        return operator.matchLiteral(actual, expected, this);

    }

    @Override
    public boolean fail(String reason) {
        if (failReason == null) {
            failReason = reason;
        } else {
            failReason = " | " + reason;
        }      
        failures.addFailure(this);
        return false;
    }

    public static MatchingOperation of(Object actual, Object expected, Match.Context context, MatchingOperator operator, Failures failures) {
        return new MatchingOperation(actual, expected, context, operator, failures);
    }

    public MatchingOperation ofLegacy(Match.Context context, Match.Type type, Match.Value actual, Match.Value expected) {
        return of(actual.getValue(), expected.getValue(), context, type.operator(), failures);
    }
}

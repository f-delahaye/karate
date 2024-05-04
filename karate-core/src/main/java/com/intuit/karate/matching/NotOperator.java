package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

// TODO add assertion to make sure only ONE method is called, or else we may end up with NOT NOT.
public class NotOperator implements MatchingOperator {

    private final MatchingOperator delegate;
    
    public NotOperator(MatchingOperator delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matchList(List<?> actualList, Object expected, MatchingOperation operation) {
        if (delegate.matchList(actualList, expected, childOperation(actualList, expected, operation))) {
            return fail(operation);
        }
        return true;
    }

    @Override
    public boolean matchObject(Map<String, ?> actualObject, Object expected, MatchingOperation operation) {
        if (delegate.matchObject(actualObject, expected, childOperation(actualObject, expected, operation))) {
            return fail(operation);
        }
        return true;
    }

    @Override
    public boolean matchLiteral(Object actualLiteral, Object expected, MatchingOperation operation) {
        if (delegate.matchLiteral(actualLiteral, expected, childOperation(actualLiteral, expected, operation))) {
            return fail(operation);
        }
        return true;
    }

    @Override
    public boolean matchMacro(Object actual, String macro, MatchingOperation operation) {
        //THe ! operation is honoured in case macro is a regular string which happens to start with #, or a validator.
        // If it's a real macro, ! should NOT be used, as per doc, macro forces equals. 
        if (delegate.matchMacro(actual, macro, childOperation(actual, macro, operation))) {
            return fail(operation);
        }
        return true;

    }

    

    // Creates a child operation which
    // - uses delegate, instead of this, as the operator so that Not is not propagated to sub calls.
    // - will capture any failures and throw them away i.e. if delegate fails, this operator must pass. If a failure is recorded in delegate, we don't want that failure in parentOperation. 
    private MatchingOperation childOperation(Object actual, Object expected, MatchingOperation parentOperation) {
        return MatchingOperation.of(actual, expected, parentOperation.context, delegate, parentOperation.failures);
    }

    // Implementing Not operator as a wrapper makes it very easy to mix it with any other core comparators, including Each.
    // However, the downside is that all failures will have the same message, as we don't know whether an equals, or a each contains, failed.
    private boolean fail(MatchingOperation operation) {
        return operation.fail("actual matches expected");
    }
}

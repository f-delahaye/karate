package com.intuit.karate.match;

import java.util.Map;

// TODO add assertion to make sure only ONE method is called, or else we may end up with NOT NOT.
public class NotOperator implements Operator {

    private final Operator delegate;
    private final boolean emptyExpectedMapPasses;

    public NotOperator(Operator delegate, boolean emptyExpectedMapPasses) {
        this.delegate = delegate;
        // match some_map not contains {}
        // must apparently pass (even though match some_map contains {} also passes).
        // LegacyMatchOperation would have a hack for that, and so does Operator.
        // Note that whether true or false should be passed is determined by construction based on the delegate, as this hack
        // apparently only applies for Contains.
        this.emptyExpectedMapPasses = emptyExpectedMapPasses;
    }



    // Implementing Not operator as a wrapper makes it very easy to mix it with any other core comparators, including Each.
    // However, the downside is that all failures will have the same message, as we don't know whether an equals, or a each contains, failed.
    protected boolean fail(MatchOperation operation) {
        return operation.fail("actual " + (delegate instanceof ContainsOperator ? "contains":"equals")+" expected");
    }

    @Override
    public boolean execute(MatchOperation operation) {
        if (emptyExpectedMapPasses && operation.expected.isMap() && operation.expected.<Map>getValue().isEmpty()) {
            return operation.pass();
        }
        if (delegate.execute(operation)) {
            return fail(operation);
        }
        return operation.pass();
    }
}

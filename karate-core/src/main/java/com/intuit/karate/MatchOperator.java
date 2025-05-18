package com.intuit.karate;

public interface MatchOperator {

    class EachOperator implements MatchOperator {
        final MatchOperator delegate;

        EachOperator(MatchOperator delegate) {
            this.delegate = delegate;
        }

        public String toString() {
            return "EACH_"+delegate;
        }
    }

    class NotOperator implements MatchOperator {
        final CoreOperator delegate;
        final String failureMessage;

        NotOperator(CoreOperator delegate, String failureMessage) {
            this.delegate = delegate;
            this.failureMessage = failureMessage;
        }

        public String toString() {
            return "NOT_"+delegate;
        }
    }

    class CoreOperator implements MatchOperator {

        static final CoreOperator EQUALS = new CoreOperator(true, false, false, false, false);
        static final CoreOperator CONTAINS = new CoreOperator(false, true, false, false, false);
        static final CoreOperator CONTAINS_ANY = new CoreOperator(false, false, true, false, false);
        static final CoreOperator CONTAINS_ONLY = new CoreOperator(false, false, false, true, false);

        private final boolean isEquals;
        private final boolean isContains;
        private final boolean isContainsAny;
        private final boolean isContainsOnly;
        private final boolean isDeep;

        private CoreOperator(boolean isEquals, boolean isContains, boolean isContainsAny, boolean isContainsOnly) {
            this(isEquals, isContains, isContainsAny, isContainsOnly, false);
        }

        private CoreOperator(boolean isEquals, boolean isContains, boolean isContainsAny, boolean isContainsOnly, boolean isDeep) {
            this.isEquals = isEquals;
            this.isContains = isContains;
            this.isContainsAny = isContainsAny;
            this.isContainsOnly = isContainsOnly;
            this.isDeep = isDeep;
        }

        CoreOperator deep() {
            return new CoreOperator(isEquals, isContains, isContainsAny, isContainsOnly, true);
        }

        boolean isEquals() {
            return isEquals;
        }

        boolean isContains() {
            return isContains;
        }

        boolean isContainsAny() {
            return isContainsAny;
        }

        boolean isContainsOnly() {
            return isContainsOnly;
        }

        boolean isContainsFamily() {
            return isContains() || isContainsOnly() || isContainsAny();
        }

        MatchOperator childOperator(Match.Value value) {
            // TODO why force equals here?
            // match [['foo'], ['bar']] contains deep 'fo'
            // will fail if leaves are matched with equals, but should it not pass?
            return isDeep && value.isMapOrListOrXml()?this:EQUALS;
        }

        /**
         * The operator specified by the user (^, ^+, ...) is provided as the {@code specifiedOperator} parameter.
         * However, when using one of Contains Family operators, it may require some adjustments.
         *
         * <p>Example:
         * <pre>{@code
         * def actual = [{ a: 1, b: 'x' }, { a: 2, b: 'y' }]
         * def part = { a: 1 }
         * match actual contains '#(^part)'
         * }</pre>
         *
         * In this example, {@code specifiedOperator} is {@code Contains}. However:
         * <ul>
         *   <li>The specified operator ({@code ^}) is applied when processing the list.</li>
         *   <li>Child operators are applied when processing objects within the list.</li>
         * </ul>
         *
         * According to {@link #childOperator(Match.Value)}, {@code Contains}' child operator is {@code Equals}.
         * As a result, the code attempts to match {@code { a: 1, b: 'x' } equals { a: 1 }}, which fails.
         *
         * <p>What we actually want is to preserve both {@code Contains} operators:
         * <ul>
         *   <li>The one from the match instruction.</li>
         *   <li>The one implied by the macro logic.</li>
         * </ul>
         * This method achieves that by creating a custom operator that effectively applies two {@code Contains} operations.
         *
         * <p>Note: If a third level of matching is required (e.g., the objects in {@code actual} contain other objects),
         * it would fall back to the child operator of the child operator, which is {@code Equals}.
         * This differs from the legacy implementation, which would enforce a deep {@code Contains},
         * potentially triggering issue #2515.
         *
         * <p>That said, {@code Contains Deep} may still be specified explicitly by the user,
         * for example, to handle nested structures like objects within objects within lists.
         */
        protected MatchOperator macroOperator(MatchOperator specifiedOperator) {
            if (isContainsFamily()) {
                return isDeep ? this : new CoreOperator(false, isContains(), isContainsAny(), isContainsOnly()) {
                    protected MatchOperator childOperator(Match.Value actual) {
                        return specifiedOperator;
                    }
                };
            }
            return specifiedOperator;
        }

        public String toString() {
            String operatorString = isEquals?"EQUALS":isContains?"CONTAINS":isContainsAny?"CONTAINS_ANY":"CONTAINS_ONLY";
            return isDeep?operatorString+"_DEEP":operatorString;
        }


    }
}

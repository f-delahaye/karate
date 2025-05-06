package com.intuit.karate.match;

import com.intuit.karate.XmlUtils;

import java.util.List;
import java.util.Map;

public class ContainsOperator extends AbstractOperator {

//    public static final ContainsOperator CONTAINS = new ContainsOperator(false, false, false);
//    public static final ContainsOperator CONTAINS_DEEP = new ContainsOperator(false, false, true);
//    public static final NotOperator NOT_CONTAINS = new EmptyMapAwareNotOperator(CONTAINS);
//    public static final NotOperator NOT_CONTAINS_DEEP = new NotOperator(CONTAINS_DEEP);
//
//    public static final ContainsOperator CONTAINS_ANY = new ContainsOperator(true, false, false);
//    public static final ContainsOperator CONTAINS_ANY_DEEP = new ContainsOperator(true, false, true);
//    public static final NotOperator NOT_CONTAINS_ANY = new NotOperator(CONTAINS_ANY);
//    public static final NotOperator NOT_CONTAINS_ANY_DEEP = new NotOperator(CONTAINS_ANY_DEEP);
//
//    public static final ContainsOperator CONTAINS_ONLY = new ContainsOperator(false, true, false);
//    public static final ContainsOperator CONTAINS_ONLY_DEEP = new ContainsOperator(false, true, true);
//    public static final NotOperator NOT_CONTAINS_ONLY = new NotOperator(CONTAINS_ONLY);
//    public static final NotOperator NOT_CONTAINS_ONLY_DEEP = new NotOperator(CONTAINS_ONLY_DEEP);


    private final boolean any;
    private final boolean only;
    private final boolean deep;
    private final EqualsOperator equalsOperator;

    public ContainsOperator(boolean any, boolean only, boolean deep, EqualsOperator equalsOperator) {
        super(equalsOperator.matchEachEmptyAllowed);
        this.any = any;
        this.only = only;
        this.deep = deep;
        this.equalsOperator = equalsOperator;
    }

    private boolean isAny(MatchOperation operation) {
        return any;
    }

    private boolean isOnly(MatchOperation operation) {
        return only;
    }

    @Override
    public boolean executeNonMacro(MatchOperation operation) {
        return doExecuteNonMacro(operation) ? operation.pass() :  operation.fail("actual does not contain expected");
    }

    private boolean doExecuteNonMacro(MatchOperation operation) {
        Match.Value actual = operation.actual;
        Match.Value expected = operation.expected;
        Match.Context context = operation.context;
            switch (actual.type) {
                case STRING:
                    String actString = actual.getValue();
                    String expString = expected.getValue();
                    return actString.contains(expString);
                case LIST:
                    List actList = actual.getValue();
                    List expList = expected.getValue();
                    int actListCount = actList.size();
                    int expListCount = expList.size();
                    // visited array used to handle duplicates
                    boolean[] actVisitedList = new boolean[actListCount];
                    if (!isAny(operation) && expListCount > actListCount) {
                        return operation.fail("actual array length is less than expected - " + actListCount + ":" + expListCount);
                    }
                    if (isOnly(operation) && expListCount != actListCount) {
                        return operation.fail("actual array length is not equal to expected - " + actListCount + ":" + expListCount);
                    }
                    for (Object exp : expList) { // for each item in the expected list
                        boolean found = false;
                        Match.Value expListValue = new Match.Value(exp);
                        for (int i = 0; i < actListCount; i++) {
                            Match.Value actListValue = new Match.Value(actList.get(i));
                            Operator childOperator = childOperator(actListValue);

                            MatchOperation mo = new MatchOperation(context.descend(i), actListValue, expListValue);
                            childOperator.execute(mo);
                            if (mo.pass) {
                                if (isAny(operation)) {
                                    return true; // exit early
                                }
                                // contains only : If element is found also check its occurrence in actVisitedList
                                else if(isOnly(operation)) {
                                    // if not yet visited
                                    if(!actVisitedList[i]) {
                                        // mark it visited
                                        actVisitedList[i]  = true;
                                        found = true;
                                        break; // next item in expected list
                                    }
                                    // else do nothing does not consider it a match
                                }
                                else {
                                    found = true;
                                    break; // next item in expected list
                                }
                            }
                        }
                        if (!found && !isAny(operation)) {
                            // if we reached here, all items in the actual list were scanned
                            if (isOnly(operation)) {
                                // #2644 clear nested errors for contains only
                                operation.failures.clear();
                            }
                            return operation.fail("actual array does not contain expected item - " + expListValue.getAsString());
                        }
                    }
                    if (isAny(operation)) {
                        return operation.fail("actual array does not contain any of the expected items");
                    }
                    return true; // if we reached here, all items in the expected list were found
                case MAP:
                    Map<String, Object> actMap = actual.getValue();
                    Map<String, Object> expMap = expected.getValue();
                    return matchMapValues(actMap, expMap, operation, false, isOnly(operation), isAny(operation));
                case XML:
                    Map<String, Object> actXml = (Map) XmlUtils.toObject(actual.getValue());
                    Map<String, Object> expXml = (Map) XmlUtils.toObject(expected.getValue());
                    return matchMapValues(actXml, expXml, operation, false, isOnly(operation), isAny(operation));
                default:
                    return equalsOperator.execute(operation);
            }

    }

    /**
     * Returns the operator to be used for nested processing e.g objects within lists or objects within objects.
     */
    // Note it is calculated dynamically, and not at creation time (in the constructor), because it actually depends
    // on actual.isMapOrListOrXml
    @Override
    protected Operator childOperator(Match.Value actual) {
        return deep && actual.isMapOrListOrXml()?this: equalsOperator;
    }

    /**
     * Hook to adjust the operator used for macro.
     * <p>
     * Whatever operator the user specified (^, ^+, ...) will be supplied as the specifiedOperator parameter.
     * However, the Contains operator may need to tweak it a little bit.
     * <p>
     * Given
     * * def actual = [{ a: 1, b: 'x' }, { a: 2, b: 'y' }]
     * * def part = { a: 1 }
     * * match actual contains '#(^part)'
     * <p>
     * specifiedOperator will be Contains. However, in this example:
     * - the specified operator will be applied while processing the list
     * - child operators will be applied while processing the objects within the list.
     * And per {@link #childOperator(Match.Value)}, Contains' child operators are Equals, so the code would end up
     * trying to match { a: 1, b: 'x' } equals { a: 1 }, which would fail.
     * <p>
     * What we really want here is to keep both Contains, the one from the match instruction and the one from the macro.
     * This method does just that by creating a custom Operator that will apply 2 contains.
     * <p>
     * Note that should a third processing be needed (e.g. because the objects in actual contain other objects),
     * it would use the child operator of the child operator, which would be Equals.
     * This behavior differs from the Legacy implementation that would force a Deep Contains which would in turn cause issue #2515.
     * <p>
     * However, Contains Deep may still be specified at user's discretion e.g. to handle objects in objects in lists.
     */
    @Override
    protected Operator macroOperator(Operator specifiedOperator) {
        return deep ? this : new ContainsOperator(any, only, false, equalsOperator) {
            protected Operator childOperator(Match.Value actual) {
                return specifiedOperator;
            }
        };
    }
}

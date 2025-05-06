package com.intuit.karate.match;

import com.intuit.karate.JsonUtils;
import com.intuit.karate.StringUtils;
import com.intuit.karate.XmlUtils;
import com.intuit.karate.graal.JsValue;

import java.util.*;

import static com.intuit.karate.match.LegacyMatchOperation.*;

/**
 * {@link AbstractOperator} (along with its subclasses) contain the core logic of match operations.
 * <p>
 * This class is implemented by {@link ContainsOperator} and {@link EqualsOperator}.
 * Other subclasses of {@link Operator} such as {@link NotOperator} and {@link EachOperator} are more decorators around other Operators and
 * should NOT implement this class.
 * <p>
 * It currently handles most of the macroEqualsExpected and matchMapValues methods, as well as type conversions and most of the optional stuff.
 * Subclasses only need to implement {@link #executeNonMacro(MatchOperation)}, {@link #macroOperator(Operator)} and {@link #childOperator(Match.Value)}.
 * <p>
 *
 * This class and its subclasses cannot be singletons because of the matchEachEmptyAllowed, which is match-operation specific, although conceptually, they could be.
 *
 */

public abstract class AbstractOperator implements Operator {

    protected final boolean matchEachEmptyAllowed;

    protected AbstractOperator(boolean matchEachEmptyAllowed) {
        this.matchEachEmptyAllowed = matchEachEmptyAllowed;
    }

    @Override
    public boolean execute(MatchOperation operation) {
        Match.Value expected = operation.expected;
        Match.Value actual = operation.actual;
        Match.Context context = operation.context;
        if (actual.isNotPresent()) {
            if (!expected.isString() || !expected.getAsString().startsWith("#")) {
                return operation.fail("actual path does not exist");
            }
        }

        if (expected.type != actual.type) {
            if (expected.isXml() && actual.isMap()) {
                // special case, auto-convert rhs
                MatchOperation mo = new MatchOperation(context, actual, new Match.Value(XmlUtils.toObject(expected.getValue(), true)));
                this.execute(mo);
                return mo.pass ? operation.pass() : operation.fail(mo.failReason);
            }
            if (this instanceof ContainsOperator && !expected.isList() && !(expected.isString() && expected.isArrayObjectOrReference())) {
                MatchOperation mo = new MatchOperation(context, actual, new Match.Value(Collections.singletonList(expected.getValue())));
                this.execute(mo);
                return mo.pass ? operation.pass() : operation.fail(mo.failReason);
            }
        }

        if (expected.isString() && expected.getAsString().startsWith("#")) {
            return macroEqualsExpected(operation.expected.getValue(), operation) ? operation.pass() : operation.fail(null);
        }
        return executeNonMacro(operation);
    }

    /**
     * Any operations which don't start with #, or which are regular Strings that happen to start with a #, will call this method.
     * It is expected to contain operation-specific logic.
     */
    protected abstract boolean executeNonMacro(MatchOperation operation);

    protected boolean macroEqualsExpected(String macro, MatchOperation operation) {
        Match.Value actual = operation.actual;
        Match.Context context = operation.context;
        boolean optional = macro.startsWith("##");
        if (optional && actual.isNull()) { // exit early
            return true;
        }
        int minLength = optional ? 3 : 2;
        if (macro.length() > minLength) {
            macro = macro.substring(minLength - 1);
            if (macro.startsWith("(") && macro.endsWith(")")) {
                macro = macro.substring(1, macro.length() - 1);
                com.intuit.karate.Match.Type nestedType = macroToMatchType(false, macro);
                int startPos = matchTypeToStartPos(nestedType);
                Operator nestedOperator = nestedType.operator(matchEachEmptyAllowed);
                macro = macro.substring(startPos);
                context.JS.put("$", context.root.actual.getValue());
                context.JS.put("_", actual.getValue());
                JsValue jv = context.JS.eval(macro);
                context.JS.bindings.removeMember("$");
                context.JS.bindings.removeMember("_");
                MatchOperation mo = new MatchOperation(context, actual, new Match.Value(jv.getValue()));
                return macroOperator(nestedOperator).execute(mo);
            } else if (macro.startsWith("[")) {
                int closeBracketPos = macro.indexOf(']');
                if (closeBracketPos != -1) { // array, match each
                    if (closeBracketPos > 1) {
                        if (!actual.isList()) {
                            return operation.fail("actual is not an array");
                        }
                        String bracketContents = macro.substring(1, closeBracketPos);
                        List listAct = actual.getValue();
                        int listSize = listAct.size();
                        context.JS.put("$", context.root.actual.getValue());
                        context.JS.put("_", listSize);
                        String sizeExpr;
                        if (containsPlaceholderUnderscore(bracketContents)) { // #[_ < 5]
                            sizeExpr = bracketContents;
                        } else { // #[5] | #[$.foo]
                            sizeExpr = bracketContents + " == _";
                        }
                        JsValue jv = context.JS.eval(sizeExpr);
                        context.JS.bindings.removeMember("$");
                        context.JS.bindings.removeMember("_");
                        if (!jv.isTrue()) {
                            return operation.fail("actual array length is " + listSize);
                        }
                    }
                    if (macro.length() > closeBracketPos + 1) {
                        macro = StringUtils.trimToNull(macro.substring(closeBracketPos + 1));
                        if (macro != null) {
                            if (macro.startsWith("(") && macro.endsWith(")")) {
                                macro = macro.substring(1, macro.length() - 1); // strip parens
                            }
                            if (macro.startsWith("?")) { // #[]? _.length == 3
                                macro = "#" + macro;
                            }
                            if (macro.startsWith("#")) {
                                MatchOperation mo = new MatchOperation(context, actual, new Match.Value(macro));
                                com.intuit.karate.Match.Type.EACH_EQUALS.operator(matchEachEmptyAllowed).execute(mo);
                                return mo.pass ? operation.pass() : operation.fail("all array elements matched");
                            } else { // schema reference
                                com.intuit.karate.Match.Type nestedType = macroToMatchType(true, macro); // match each
                                int startPos = matchTypeToStartPos(nestedType);
                                macro = macro.substring(startPos);
                                JsValue jv = context.JS.eval(macro);
                                MatchOperation mo = new MatchOperation(context, actual, new Match.Value(jv.getValue()));
                                return nestedType.operator(matchEachEmptyAllowed).execute(mo);
                            }
                        }
                    }
                    return true; // expression within square brackets is ok
                }
            } else { // '#? _ != 0' | '#string' | '#number? _ > 0'
                int questionPos = macro.indexOf('?');
                String validatorName = null;
                // in case of regex we don't want to remove the '?'
                if (questionPos != -1 && !macro.startsWith(REGEX)) {
                    validatorName = macro.substring(0, questionPos);
                    if (macro.length() > questionPos + 1) {
                        macro = StringUtils.trimToEmpty(macro.substring(questionPos + 1));
                    } else {
                        macro = "";
                    }
                } else {
                    validatorName = macro;
                    macro = "";
                }
                validatorName = StringUtils.trimToNull(validatorName);
                if (validatorName != null) {
                    Match.Validator validator = null;
                    if (validatorName.startsWith(REGEX)) {
                        String regex = validatorName.substring(5).trim();
                        validator = new Match.RegexValidator(regex);
                    } else {
                        validator = Match.VALIDATORS.get(validatorName);
                    }
                    if (validator != null) {
                        if (optional && (actual.isNotPresent() || actual.isNull())) {
                            // pass
                        } else if (!optional && actual.isNotPresent()) {
                            // if the element is not present the expected result can only be
                            // the notpresent keyword, ignored or an optional comparison
                            return operation.expected.isNotPresent() || "#ignore".contentEquals(operation.expected.getAsString());
                        } else {
                            Match.Result mr = validator.apply(actual);
                            if (!mr.pass) {
                                return operation.fail(mr.message);
                            }
                        }
                    } else if (!validatorName.startsWith(REGEX)) { // expected is a string that happens to start with "#"
                        return executeNonMacro(operation);
                    }

                }
                macro = StringUtils.trimToNull(macro);
                if (macro != null && questionPos != -1) {
                    context.JS.put("$", context.root.actual.getValue());
                    context.JS.put("_", actual.getValue());
                    JsValue jv = context.JS.eval(macro);
                    context.JS.bindings.removeMember("$");
                    context.JS.bindings.removeMember("_");
                    if (!jv.isTrue()) {
                        return operation.fail("evaluated to 'false'");
                    }
                }
            }
        }
        return true; // all ok
    }

    protected boolean matchMapValues(Map<String, Object> actMap, Map<String, Object> expMap, MatchOperation operation, boolean isEquals, boolean isContainsOnly, boolean isContainsAny) {
        boolean isContains = !isEquals && !isContainsAny && !isContainsOnly;
        if (actMap.size() > expMap.size() && (isEquals || isContainsOnly)) {
            int sizeDiff = actMap.size() - expMap.size();
            Map<String, Object> diffMap = new LinkedHashMap(actMap);
            for (String key : expMap.keySet()) {
                diffMap.remove(key);
            }
            return operation.fail("actual has " + sizeDiff + " more key(s) than expected - " + JsonUtils.toJson(diffMap));
        }
        Set<String> unMatchedKeysAct = new LinkedHashSet(actMap.keySet());
        Set<String> unMatchedKeysExp = new LinkedHashSet(expMap.keySet());
        for (Map.Entry<String, Object> expEntry : expMap.entrySet()) {
            String key = expEntry.getKey();
            Object childExp = expEntry.getValue();
            if (!actMap.containsKey(key)) {
                if (childExp instanceof String) {
                    String childString = (String) childExp;
                    if (childString.startsWith("##") || childString.equals("#ignore") || childString.equals("#notpresent")) {
                        if (isContainsAny) {
                            return true; // exit early
                        }
                        unMatchedKeysExp.remove(key);
                        if (unMatchedKeysExp.isEmpty()) {
                            if (isContains) {
                                return true; // all expected keys matched
                            }
                        }
                        continue;
                    }
                }
                if (!isContainsAny) {
                    return operation.fail("actual does not contain key - '" + key + "'");
                }
            }
            Match.Value childActValue = new Match.Value(actMap.get(key));
            MatchOperation mo = new MatchOperation(operation.context.descend(key), childActValue, new Match.Value(childExp));
            childOperator(childActValue).execute(mo);
            if (mo.pass) {
                if (isContainsAny) {
                    return true; // exit early
                }
                unMatchedKeysExp.remove(key);
                if (unMatchedKeysExp.isEmpty()) {
                    if (isContains) {
                        return true; // all expected keys matched
                    }
                }
                unMatchedKeysAct.remove(key);
            } else if (isEquals) {
                return operation.fail("match failed for name: '" + key + "'");
            }
        }
        if (isContainsAny) {
            return unMatchedKeysExp.isEmpty() ? true : operation.fail("no key-values matched");
        }
        if (unMatchedKeysExp.isEmpty()) {
            if (isContains) {
                return true; // all expected keys matched, expMap was empty in the first place
            }
        }
        if (!unMatchedKeysExp.isEmpty()) {
            return operation.fail("all key-values did not match, expected has un-matched keys - " + unMatchedKeysExp);
        }
        if (!unMatchedKeysAct.isEmpty()) {
            return operation.fail("all key-values did not match, actual has un-matched keys - " + unMatchedKeysAct);
        }
        return true;
    }

    protected abstract Operator childOperator(Match.Value actual);


    protected abstract Operator macroOperator(Operator operator);
}

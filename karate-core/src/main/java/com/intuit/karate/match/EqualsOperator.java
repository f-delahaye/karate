package com.intuit.karate.match;

import com.intuit.karate.XmlUtils;

import java.math.BigDecimal;
import java.util.*;

import static com.intuit.karate.match.MatchOperation.toBigDecimal;

public class EqualsOperator extends AbstractOperator {

    public EqualsOperator(boolean matchEachEmptyAllowed) {
        super(matchEachEmptyAllowed);
    }

    @Override
    public boolean executeNonMacro(MatchOperation operation) {
        // TODO Hack alert LegacyMatchOperation would handle this internally in execute, validate that postponing it until operator.execute is fine
        if (operation.actual.type != operation.expected.type) {
            return operation.fail("data types don't match");
        }
        return doExecuteNonMacro(operation) ? operation.pass() : operation.fail("not equal");
    }

    private boolean doExecuteNonMacro(MatchOperation operation) {
        Match.Value actual = operation.actual;
        Match.Value expected = operation.expected;
        Match.Context context = operation.context;

        switch (actual.type) {
            case NULL:
                return true; // both are null
            case BOOLEAN:
                boolean actBoolean = actual.getValue();
                boolean expBoolean = expected.getValue();
                return actBoolean == expBoolean;
            case NUMBER:
                if (actual.getValue() instanceof BigDecimal || expected.getValue() instanceof BigDecimal) {
                    BigDecimal actBigDecimal = toBigDecimal(actual.getValue());
                    BigDecimal expBigDecimal = toBigDecimal(expected.getValue());
                    return actBigDecimal.compareTo(expBigDecimal) == 0;
                } else {
                    Number actNumber = actual.getValue();
                    Number expNumber = expected.getValue();
                    return actNumber.doubleValue() == expNumber.doubleValue();
                }
            case STRING:
                return actual.getValue().equals(expected.getValue());
            case BYTES:
                byte[] actBytes = actual.getValue();
                byte[] expBytes = expected.getValue();
                return Arrays.equals(actBytes, expBytes);
            case LIST:
                List actList = actual.getValue();
                List expList = expected.getValue();
                int actListCount = actList.size();
                int expListCount = expList.size();
                if (actListCount != expListCount) {
                    return operation.fail("actual array length is not equal to expected - " + actListCount + ":" + expListCount);
                }
                for (int i = 0; i < actListCount; i++) {
                    Match.Value actListValue = new Match.Value(actList.get(i));
                    Match.Value expListValue = new Match.Value(expList.get(i));
                    MatchOperation mo = new MatchOperation(context.descend(i), actListValue, expListValue);
                    this.execute(mo);
                    if (!mo.pass) {
                        return operation.fail("array match failed at index " + i);
                    }
                }
                return true;
            case MAP:
                Map<String, Object> actMap = actual.getValue();
                Map<String, Object> expMap = expected.getValue();
                // Equals will always use self for child operations
                return matchMapValues(actMap, expMap, operation,true, false, false);
            case XML:
                Map<String, Object> actXml = (Map) XmlUtils.toObject(actual.getValue(), true);
                Map<String, Object> expXml = (Map) XmlUtils.toObject(expected.getValue(), true);
                return matchMapValues(actXml, expXml, operation, true, false, false);
            case OTHER:
                return actual.getValue().equals(expected.getValue());
            default:
                throw new RuntimeException("unexpected type (match equals): " + actual.type);
        }
    }

    @Override
    protected Operator childOperator(Match.Value actual) {
        return this;
    }

    @Override
    protected Operator macroOperator(Operator operator) {
        return operator;
    }
}

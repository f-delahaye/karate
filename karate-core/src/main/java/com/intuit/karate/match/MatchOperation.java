package com.intuit.karate.match;

import com.intuit.karate.StringUtils;
import com.intuit.karate.graal.JsEngine;

import java.math.BigDecimal;
import java.util.*;

public class MatchOperation {
    final com.intuit.karate.Match.Type type;
    final Match.Value actual;
    final Match.Value expected;
    final List<MatchOperation> failures;
    final Match.Context context;
    boolean pass = true;
    String failReason;


    public MatchOperation(Match.Context context, Match.Value actual, Match.Value expected) {
        this(null, actual, expected, context, null);
    }

    public MatchOperation(com.intuit.karate.Match.Type type, Match.Value actual, Match.Value expected, Match.Context context, JsEngine js) {
        this.type = type;
        this.actual = actual;
        this.expected = expected;
        if (context == null) {
            if (js == null) {
                js = JsEngine.global();
            }
            this.failures = new ArrayList();
            if (actual.isXml()) {
                this.context = new Match.Context(js, this, true, 0, "/", "", -1);
            } else {
                this.context = new Match.Context(js, this, false, 0, "$", "", -1);
            }
        } else {
            this.context = context;
            this.failures = context.root.failures;
        }
    }

    public static BigDecimal toBigDecimal(Object o) {
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        } else if (o instanceof Number) {
            Number n = (Number) o;
            return BigDecimal.valueOf(n.doubleValue());
        } else {
            throw new RuntimeException("expected number instead of: " + o);
        }
    }

    private boolean isXmlAttributeOrMap() {
        return context.xml && actual.isMap()
                && (context.name.equals("@") || actual.<Map>getValue().containsKey("_"));
    }

    private static String collectFailureReasons(MatchOperation root) {
        StringBuilder sb = new StringBuilder();
        sb.append("match failed: ").append(root.type).append('\n');
        Collections.reverse(root.failures);
        Iterator<MatchOperation> iterator = root.failures.iterator();
        Set previousPaths = new HashSet();
        int index = 0;
        int prevDepth = -1;
        while (iterator.hasNext()) {
            MatchOperation mo = iterator.next();
            if (previousPaths.contains(mo.context.path) || mo.isXmlAttributeOrMap()) {
                continue;
            }
            previousPaths.add(mo.context.path);
            if (mo.context.depth != prevDepth) {
                prevDepth = mo.context.depth;
                index++;
            }
            String prefix = StringUtils.repeat(' ', index * 2);
            sb.append(prefix).append(mo.context.path).append(" | ").append(mo.failReason);
            sb.append(" (").append(mo.actual.type).append(':').append(mo.expected.type).append(")");
            sb.append('\n');
            if (mo.context.xml) {
                sb.append(prefix).append(mo.actual.getAsXmlString()).append('\n');
                sb.append(prefix).append(mo.expected.getAsXmlString()).append('\n');
            } else {
                Match.Value expected = mo.expected.getSortedLike(mo.actual);
                sb.append(prefix).append(mo.actual.getWithinSingleQuotesIfString()).append('\n');
                sb.append(prefix).append(expected.getWithinSingleQuotesIfString()).append('\n');
            }
            if (iterator.hasNext()) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    boolean pass() {
        pass = true;
        return true;
    }

    boolean fail(String reason) {
        pass = false;
        if (reason == null) {
            return false;
        }
        failReason = failReason == null ? reason : reason + " | " + failReason;
        context.root.failures.add(this);
        return false;
    }

    String getFailureReasons() {
        return MatchOperation.collectFailureReasons(this);
    }
}

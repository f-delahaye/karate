/*
 * The MIT License
 *
 * Copyright 2022 Karate Labs Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate;

import com.intuit.karate.graal.JsEngine;
import com.intuit.karate.match.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author pthomas3
 */
public class Match extends com.intuit.karate.match.Match{

    public static enum Type {

        EQUALS,
        NOT_EQUALS,
        CONTAINS,
        NOT_CONTAINS,
        CONTAINS_ONLY,
        NOT_CONTAINS_ONLY,
        CONTAINS_ANY,
        NOT_CONTAINS_ANY,
        CONTAINS_DEEP,
        NOT_CONTAINS_DEEP,
        CONTAINS_ONLY_DEEP,
        NOT_CONTAINS_ONLY_DEEP,
        CONTAINS_ANY_DEEP,
        NOT_CONTAINS_ANY_DEEP,
        EACH_EQUALS,
        EACH_NOT_EQUALS,
        EACH_CONTAINS,
        EACH_NOT_CONTAINS,
        EACH_NOT_CONTAINS_ONLY,
        EACH_CONTAINS_ONLY,
        EACH_CONTAINS_ANY,
        EACH_CONTAINS_DEEP;

        private boolean isEach() {
            return name().startsWith("EACH");
        }

        private boolean isContains() {
            return name().contains("CONTAINS");
        }

        private boolean isAny() {
            return name().contains("ANY");
        }

        private boolean isOnly() {
            return name().contains("ONLY");
        }

        private boolean isDeep() {
            return name().endsWith("DEEP");
        }

        private boolean isNot() {
            return name().contains("NOT_");
        }

        public Operator operator(boolean matchEachEmptyAllowed) {
            EqualsOperator equalsOperator = new EqualsOperator(matchEachEmptyAllowed);
            Operator operator = isContains()? new ContainsOperator(isAny(), isOnly(), isDeep(), equalsOperator) : equalsOperator;
            if (isNot()) {
                boolean strictContains = isContains() && !isAny()&&!isOnly();
                operator =  new NotOperator(operator, strictContains);
            }
            if (isEach()) {
                operator = new EachOperator(matchEachEmptyAllowed, operator);
            }
            return operator;
        }
    }

    public static class Result {

        public final String message;
        public final boolean pass;

        protected Result(boolean pass, String message) {
            this.pass = pass;
            this.message = message;
        }

        @Override
        public String toString() {
            return pass ? "[pass]" : message;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap(2);
            map.put("pass", pass);
            map.put("message", message);
            return map;
        }

    }

    public static class Value extends com.intuit.karate.match.Match.Value {

        protected Value(Object value) {
            super(value);
        }

        protected Value(Object value, boolean exceptionOnMatchFailure) {
            super(value, exceptionOnMatchFailure);
        }
    }

    public static Result execute(JsEngine js, Type matchType, Object actual, Object expected, boolean matchEachEmptyAllowed) {
        return com.intuit.karate.match.Match.execute(js, matchType, actual, expected, matchEachEmptyAllowed, false);
    }

    public static Object parseIfJsonOrXmlString(Object o) {
        return com.intuit.karate.match.Match.parseIfJsonOrXmlString(o);
    }

    public static Value evaluate(Object actual) {
        return new Value(parseIfJsonOrXmlString(actual), false);
    }

    public static Value that(Object actual) {
        return new Value(parseIfJsonOrXmlString(actual), true);
    }

}

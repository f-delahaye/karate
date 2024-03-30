package com.intuit.karate.matching;

import java.util.List;
import java.util.Map;

import com.intuit.karate.matching.FailureCollector.FailureDescriptor;
import com.intuit.karate.matching.Match.Context;
import com.intuit.karate.matching.Match.Type;
import com.intuit.karate.matching.Match.Value;

/**
 *  Does not inherit from MatchOperation because the new word works with simple objects and operators, while the old one works with Match.Value and Match.Type.
 *  In essence, NewMatchOperation follows most of the same principles:
 * - it uses the same FailureDescriptor interface to register failure.
 * - it reuses the same processMacro method
 * 
 * One of the main differences is that it is NOT a FailureCollector itself, instead the collector is passed as a constructor parameter. This allows for better SoC, imo.
 * 
 * Also note that unlike what was stated above, this class does sometimes use Match.Value for actual. But these usages are inherited from the old world (Match.Operation parameters) that might be refactored / removed at some point. Internally; it does (or could) use regular objects.
 *  */
public class MatchHandler {

   
    public MatchHandler() {
    }

    public boolean matches(Object expected, MatchingOperator operator, Object actual, FailureCollector collector, Context context) {
        if (expected instanceof List<?> list) {
            return operator.matchArray(list, actual, collector, context);
        }
        if (expected instanceof Map map) {
            return operator.matchObject(map, actual, collector, context);
        }
        if (expected instanceof String str && str.startsWith("#")) {            
            return MatchOperation.processMacro(str, new Value(actual), context, new ExpressionParsingListenerImpl(expected, operator, actual, collector, context, this));
        }
        return operator.matchLiteral(expected, actual, collector, context);

    }

    private static class ExpressionParsingListenerImpl implements ExpressionParsingListener {

        private final FailureCollector collector;
        private final Object expected;
        private final Object actual;
        private final Context context;
        private final MatchHandler handler;
        private final MatchingOperator operator;        
                
        public ExpressionParsingListenerImpl(Object expected, MatchingOperator operator, Object actual, FailureCollector collector, Context context, MatchHandler handler) {
            this.expected = expected;
            this.operator = operator;
            this.actual = actual;
            this.context = context;
            this.collector = collector;
            this.handler = handler;
        }

        @Override
        public boolean onEachMacro(String expected) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'onEachMacro'");
        }
    
        @Override
        public boolean onShortcut(Type nestedType, Object expected) {
            return handler.matches(expected, nestedType.operator(), actual, collector, context);
        }
    
        @Override
        public boolean onRegularString(String expected) {
            return operator.matchLiteral(expected, actual, collector, context);
        }
    
        @Override
        public boolean onFail(String reason) {
            collector.addFailure(new MatchingOperation(expected, actual, context, reason));
            return false;
        }            
    }

    private static class MatchingOperation implements FailureDescriptor {


        private final Object expected;
        private final Object actual;
        private final Context context;
        private final String reason;
                
        public MatchingOperation(Object expected, Object actual, Context context, String reason) {
            this.expected = expected;
            this.actual = actual;
            this.context = context;
            this.reason = reason;
        }

        @Override
        public Value getExpected() {
            return new Match.Value(expected);
        }

        @Override
        public Value getActual() {
            return new Match.Value(actual);
        }

        @Override
        public Context getContext() {
            return context;
        }

        @Override
        public String getReason() {
            return reason;
        }    
    }    


}

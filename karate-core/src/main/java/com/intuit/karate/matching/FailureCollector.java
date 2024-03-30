package com.intuit.karate.matching;

public interface FailureCollector {
    void addFailure(FailureDescriptor descriptor);

    public String getFailureReasons();

    public static interface FailureDescriptor {
        public Match.Value getExpected();
    
        public Match.Value getActual();

        public Match.Context getContext();

        public String getReason();
    }
}

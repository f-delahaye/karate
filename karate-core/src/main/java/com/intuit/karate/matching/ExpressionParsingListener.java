package com.intuit.karate.matching;

public interface ExpressionParsingListener {


    // TODO understand the difference with onShortcut. onEachMacro seems to:
    // - not invoke JS
    // - hard code type to equals
    // - and  returns a proper message upon failure.    
    public boolean onEachMacro(String expected);

    public boolean onShortcut(Match.Type nestedType, Object expected);

    // Just a regulat string which happened to start with #
    public boolean onRegularString(String expected);

    // In some cases, the parser itself can work out a failure. A failure message will then be sent to the listener which should create a new FailureDescriptor and add it to a FailureCollector.
    public boolean onFail(String reason);
}

package com.intuit.karate.matching;

import com.intuit.karate.Match;
import com.intuit.karate.Match.Context;
import com.intuit.karate.Match.Type;
import com.intuit.karate.Match.Value;

public interface IMatchingOperation {

    Match.Value expected();

    Match.Value actual();

    Match.Context context();

    String failReason();

    boolean execute();

    boolean fail(String reason);

    interface IMatchingOperationFactory {
        IMatchingOperation create(Context context, Type type, Value actual, Value expected);
    }
}

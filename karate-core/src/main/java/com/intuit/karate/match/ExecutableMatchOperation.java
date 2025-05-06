package com.intuit.karate.match;

import com.intuit.karate.graal.JsEngine;

//Bridge between the legacy world (MatchOperation.execute()) and the new world (Operator.execute(MatchOperation)).
//Makes it easier to switch from the latter back to the former (by just replacing ExecutableMatchOperation with LegacyMatchOperation) but not sure if really needed.
public class ExecutableMatchOperation extends LegacyMatchOperation {
    public ExecutableMatchOperation(JsEngine js, Match.Type type, Match.Value actual, Match.Value expected, boolean matchEachEmptyAllowed) {
        super(js, type, actual, expected, matchEachEmptyAllowed);
    }

    public ExecutableMatchOperation(Match.Context context, Match.Type type, Match.Value actual, Match.Value expected, boolean matchEachEmptyAllowed) {
        super(context, type, actual, expected, matchEachEmptyAllowed);
    }

    @Override
    public boolean execute() {
       return type.operator(matchEachEmptyAllowed).execute(this);
    }
}

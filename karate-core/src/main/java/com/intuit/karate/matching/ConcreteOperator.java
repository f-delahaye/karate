package com.intuit.karate.matching;

/** A concrete operator actually defines matchArray/Object ... logics.
 * As opposed to {@link NotOperator} and {@link EachOperator} for example, which are merely wrappers around other Operators.
 */
public interface ConcreteOperator extends MatchingOperator{

    /**
     * Returns the operator to use for nested matchings.
     * Generally speaking:
     * - Equals operator will return self
     * - Contains operators will return Equals (contains only to one level, subsequent operations use equals) unless ...
     * - Contains operators with Deep modifier will return self.
     *  
     * Deep itself is not an operator, but a modifier.
     *   
     * Note that some nodes (typically shortcuts) may choose to use their own operator, ignoring the one returned by this method.

     * @return
     */
    MatchingOperator nestedOperator();
}

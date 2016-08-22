/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */
package me.dalemonks.contract.annotations.variables;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Variables didn't have any naming pre java 8. (In java 8 it's a compiler option
 * @author Dale Monks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface Var {
    String value();
}

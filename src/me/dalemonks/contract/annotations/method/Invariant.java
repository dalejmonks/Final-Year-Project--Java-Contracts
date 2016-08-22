/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */
package me.dalemonks.contract.annotations.method;

import me.dalemonks.contract.annotations.containers.ContainerForInvariants;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Dale Monks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD}) //Can use in method only.
@Repeatable(ContainerForInvariants.class) //This needs a holder annotation.
public @interface Invariant {
    String value(); //This uses the default value to assign the condition.
}
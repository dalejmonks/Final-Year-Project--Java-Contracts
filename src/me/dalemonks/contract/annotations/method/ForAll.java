/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */

package me.dalemonks.contract.annotations.method;

import me.dalemonks.contract.annotations.containers.ContainerForForAlls;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is called after the process runs.
 * @author Dale Monks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //Can use in method only.
@Repeatable(ContainerForForAlls.class)
public @interface ForAll {
    String value();
}

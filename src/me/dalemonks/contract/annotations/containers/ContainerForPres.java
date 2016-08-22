/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */

package me.dalemonks.contract.annotations.containers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.dalemonks.contract.annotations.method.Pre;

/**
 *
 * @author Dale Monks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //Can use in method only
public @interface ContainerForPres { //This is the holder for the pre conditions.
    Pre[] value();
}

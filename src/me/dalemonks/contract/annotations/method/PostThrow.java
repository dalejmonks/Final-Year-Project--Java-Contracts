/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */

package me.dalemonks.contract.annotations.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.dalemonks.contract.annotations.containers.ContainerForPostsThrows;

/**
 *
 * @author Dale Monks
 */
@Repeatable(ContainerForPostsThrows.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) //can use in method only.
public @interface PostThrow {
    String condition();
    Class<? extends Exception> exception();
}


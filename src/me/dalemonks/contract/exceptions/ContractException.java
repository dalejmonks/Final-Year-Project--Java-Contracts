/*
 * University of Central Lancashire
 * CO3808 - Double Project
 * Supervisor - Chris Casey
 * Project - Java Contracts
 */

package me.dalemonks.contract.exceptions;

/**
 *
 * @author Dale Monks
 */
public class ContractException extends Exception {

    public ContractException() {
        super();
    }

    public ContractException(String message) {
        super(message);
    }

    public ContractException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContractException(Throwable cause) {
        super(cause);
    }
    
}

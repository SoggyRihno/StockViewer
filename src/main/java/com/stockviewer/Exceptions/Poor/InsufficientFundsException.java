package com.stockviewer.Exceptions.Poor;

public class InsufficientFundsException extends PoorException{
    public InsufficientFundsException() {
        super("No Funds?");
    }
}

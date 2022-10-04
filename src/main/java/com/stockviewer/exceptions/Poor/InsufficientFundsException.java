package com.stockviewer.exceptions.Poor;

public class InsufficientFundsException extends PoorException{
    public InsufficientFundsException() {
        super("No Funds?");
    }
}

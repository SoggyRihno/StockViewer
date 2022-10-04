package com.stockviewer.exceptions.Poor;

public class UnknownOrderException extends PoorException{
    public UnknownOrderException(){
        super("No Orders ?");
    }
}

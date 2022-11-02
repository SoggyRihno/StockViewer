package com.stockviewer.Exceptions.Poor;

public class UnknownOrderException extends PoorException{
    public UnknownOrderException(){
        super("No Orders ?");
    }
}

package com.stockviewer.exceptions.Poor;

public class NoStockException extends PoorException{
    public NoStockException(){
        super("No Stock ?");
    }
}

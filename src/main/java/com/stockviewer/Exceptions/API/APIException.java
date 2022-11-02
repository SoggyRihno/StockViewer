package com.stockviewer.Exceptions.API;

public class APIException extends Exception{

    public APIException() {
        super("API Exception :( ");
    }
    public APIException(String message) {
        super(message);
    }


    // FIXME: 10/19/2022 
}
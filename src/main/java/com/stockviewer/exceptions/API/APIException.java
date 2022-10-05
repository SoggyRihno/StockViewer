package com.stockviewer.exceptions.API;

public class APIException extends Exception{

    public APIException() {
        super("API Exception :( ");
    }
    public APIException(String message) {
        super(message);
    }
}
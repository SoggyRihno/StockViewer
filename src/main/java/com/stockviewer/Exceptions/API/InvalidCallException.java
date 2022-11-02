package com.stockviewer.Exceptions.API;

public class InvalidCallException extends APIException{

    public InvalidCallException(String result) {
        super   (result);
    }
}

package com.stockviewer.exceptions.API;

public class InvalidCallException extends APIException{

    public InvalidCallException(String result) {
        super   (result);
    }
}

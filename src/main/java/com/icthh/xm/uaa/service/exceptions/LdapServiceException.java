package com.icthh.xm.uaa.service.exceptions;

public class LdapServiceException extends RuntimeException {
    private String errorKey;
    private String message;

    public LdapServiceException(String errorKey, String message) {
        this.errorKey = errorKey;
        this.message = message;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public void setErrorKey(String errorKey) {
        this.errorKey = errorKey;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

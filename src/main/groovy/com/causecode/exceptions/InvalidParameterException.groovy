/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.exceptions

/**
 * An exception class to represent invalid or missing data in request paramaters from the client side.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class InvalidParameterException extends Exception {

    InvalidParameterException(String message = 'Invalid Parameters', Throwable cause = new Throwable()) {
        super(message, cause)
    }
}

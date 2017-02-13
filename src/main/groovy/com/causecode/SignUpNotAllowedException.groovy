/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode

/**
 * Exception class.
 * This exception can be used to disable user signup in the installing application.
 * Installing application has to override userHookService bean as explained in UserHookService interface and then throw
 * this exception in preUserSignup hook to disable user signup.
 */
class SignUpNotAllowedException extends Exception {

    SignUpNotAllowedException(String message) {
        super(message)
    }

    SignUpNotAllowedException(String message, Throwable throwable) {
        super(message, throwable)
    }
}

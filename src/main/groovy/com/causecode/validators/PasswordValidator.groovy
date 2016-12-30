/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.validators

import grails.validation.Validateable

/**
 * This class is used as a validator for passwords while password resetting.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class PasswordValidator implements Validateable {
    String email
    String password
    String password2

    static constraints = {
        email email: true
        password password: true, validator: PASSWORD_VALIDATOR
        password2 password: true, validator: PASSWORD2_VALIDATOR
    }

    static final PASSWORD_VALIDATOR = { String password, object ->
        if (object.email && (object.email == password)) {
            return 'Password cannot be the same as the email'
        }

        if (!checkPasswordMinLength(password)) {
            return 'Password length must be at least 8 characters.'
        }

        if (!checkPasswordMaxLength(password)) {
            return 'Password length must not exceed 64 characters.'
        }

        if (!checkPasswordRegex(password)) {
            return 'Password must have at least one letter, number, and special character.'
        }
    }

    static boolean checkPasswordMinLength(String password) {
        password && password.length() >= 8
    }

    static boolean checkPasswordMaxLength(String password) {
        password && password.length() <= 64
    }

    static boolean checkPasswordRegex(String password) {
        String passValidationRegex = '^.*(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@#$%^&.]).*$'

        password && password.matches(passValidationRegex)
    }

    static final PASSWORD2_VALIDATOR = { value, object ->
        if (object.password != object.password2) {
            return 'Passwords do not match'
        }
    }
}

/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.validators

import org.apache.commons.lang.RandomStringUtils
import spock.lang.Specification
import spock.lang.Unroll

/**
 * This class specifies unit test cases for {@link com.causecode.validators.PasswordValidatorSpec}
 */
class PasswordValidatorSpec extends Specification {

    static String getRandomString() {
        int randomStringLength = 65
        String charset = (('a'..'z') + ('A'..'Z') + ('0'..'9')).join()

        return RandomStringUtils.random(randomStringLength, charset.toCharArray())
    }

    @Unroll
    void "test checkPasswordMinLength method when length of password is #length"() {
        when: 'The password string is passed to the method'
        boolean result = PasswordValidator.checkPasswordMinLength(password)

        then: 'Validation should pass based on the length'
        result == expectedResult

        where:
        length | password    | expectedResult
        5      | 'cause'     | false
        9      | 'causecode' | true
        0      | null        | false
    }

    @Unroll
    void "test checkPasswordMaxLength method when length of password is #length"() {
        when: 'The password string is passed to the method'
        boolean result = PasswordValidator.checkPasswordMaxLength(password)

        then: 'Validation should pass based on the length'
        result == expectedResult

        where:
        length | password          | expectedResult
        5      | 'cause'           | true
        9      | randomString      | false
        0      | null              | false // Will return false which is correct as true will pass the validation.
    }

    @Unroll
    void "test checkPasswordRegex method when password is #password"() {
        when: 'The password string is passed to the method'
        boolean result = PasswordValidator.checkPasswordRegex(password)

        then: 'Validation should pass based on the length'
        result == expectedResult

        where:
        password       | expectedResult
        'cause'        | false
        'CauseCode.11' | true
        '1234'         | false
        '@!$%'         | false
        null           | false
    }

    @Unroll
    void "test validator when password is #password, confirm password is #password2 and email is #email"() {
        given: 'A PasswordValidator instance'
        PasswordValidator passwordValidator = new PasswordValidator()
        passwordValidator.email = email
        passwordValidator.password = password
        passwordValidator.password2 = password2

        when: 'The instance is validated'
        boolean result = passwordValidator.validate()

        then: 'The validation should pass/fail based on the provided values'
        result == expectedResult

        where:
        password         | password2        | email            | expectedResult
        'cause'          | 'cause'          | 'cause@code.com' | false  // Min length validation failed
        'causecode'      | 'causecode'      | 'cause@code.com' | false  // No upper case, no special character
        'Causecode.11'   | 'Causecode.1'    | 'cause@code.com' | false // Passwords did not match
        'cause@code.com' | 'cause@code.com' | 'cause@code.com' | false // Password same as email
        'Causecode-11'   | 'Causecode-11'   | 'cause@code.com' | false // Regex not matched
        'CauseCode.11'   | 'CauseCode.11'   | 'cause@code.com' | true
        null             |  null            |  null            | false // Nullable checks
    }
}

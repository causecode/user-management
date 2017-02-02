/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import groovy.transform.EqualsAndHashCode

/**
 * This domain stores the spring rest tokens required for rest stateless login.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
@EqualsAndHashCode
class AuthenticationToken {

    String email
    String token

    Date dateCreated
    Date lastUpdated

    Integer accessCount = 0

    def afterLoad() {
        accessCount++
    }

    static constraints = {
        token unique: true
    }

    static mapping = {
        email index: true
        token index: true
        dateCreated index: true
        lastUpdated index: true
        version false
    }

    @Override
    String toString() {
        return "AuthenticationToken [$email][$token]"
    }
}

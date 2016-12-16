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
 * A domain class to hold all account details for users that are created by OAuth process.
 *
 * @author Vishesh Duggar
 * @author Nikhil Sharma
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
class OAuthAccount {

    User user
    OAuthProvider oAuthProvider

    // This is id that is received from the provider in the OAuth response.
    String accountId

    static constraints = {
        accountId unique: true
    }

    static mapping = {
        accountId index: true
    }

    @Override
    String toString() {
        return "OAuthAccount ($user)($oAuthProvider)"
    }
}

@SuppressWarnings(['GrailsDomainHasEquals'])
enum OAuthProvider {
    GOOGLE(0),
    LINKEDIN(1),
    FACEBOOK(2),
    TWITTER(3)

    final int id
    OAuthProvider(int id) {
        this.id = id
    }

    @Override
    String toString() {
        return "OAuthProvider (${this.name()})($id)"
    }
}

/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import groovy.util.logging.Slf4j
import javax.activity.InvalidActivityException

/**
 * This class defines default implementation of hooks during a user signup process.
 * Bean for this class has been injected as userHookService within doWithSpring closure in
 * UserManagementPlugin.groovy file.
 *
 * Installing application should create a class(say CustomUserHookService) which implements UserHookService interface
 * and then override the injected userHookService bean, and inject it for the above specified
 * class(CustomUserHookService) in resources.groovy file in order to override these methods.
 *
 * @author Ankit Agrawal
 * @since 0.0.4
 */
@Slf4j
class DefaultUserHookService implements UserHookService {

    /**
     * This web hook gets called to check if user signup feature has been enabled. By default it has been disabled.
     * Installing app needs to override this method to enable signup.
     */
    void preUserSignup() {
        log.debug 'Executing pre-user-signup hook from user-management plugin'

        throw new InvalidActivityException('User signup feature has been disabled')
    }

    /**
     * A method used to call certain method an application might need during signUp process.
     */
    @SuppressWarnings('UnusedMethodParameter')
    boolean onCreateUser(User userInstance) {
        return false
    }

    /**
     * A method which can be overridden to execute certain part of code once a new user is created with signup.
     */
    void postUserSignup() {
        return
    }
}

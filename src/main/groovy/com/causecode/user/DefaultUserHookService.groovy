/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import groovy.util.logging.Slf4j

/**
 * This class is the default implementation of UserHookService. This class is injected as userHookService bean.
 * Check Plugin descriptor file (UserManagementGrailsPlugin.groovy) for bean override.
 *
 * @author Ankit Agrawal
 * @since 0.0.4
 */
@Slf4j
class DefaultUserHookService implements UserHookService {

    /**
     * This webhook gets called to check if user signup feature has been enabled. By default it has been disabled.
     * Installing app needs to override this method to enable signup.
     */
    void preUserSignup() {
        log.debug 'Executing pre-user-signup hook from user-management plugin'

        return
    }

    /**
     * This method acts as the onCreateUser event handler. When a User is successfully created in the signup process,
     * this method is called to perform additional operations. The default implementation is empty but the installing
     * app can override and do some post creation operations here.
     *
     * @params User userInstance
     * @return boolean
     *     - true: on successful execution of operation.
     *     - false: If operation fails.
     */
    @SuppressWarnings('UnusedMethodParameter')
    boolean onCreateUser(User userInstance) {
        return false
    }

    /**
     * This method is called once the signup process is completed and User has been notified via an email.
     * This method can be used to perform post signup operations.
     */
    void postUserSignup() {
        return
    }
}

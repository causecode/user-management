/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

/**
 * This interface declares the methods required for UserHookService.
 * This plugin defines a DefaultUserHookService class which implements this interface and is registered as the
 * userHookService bean.
 * To override the default implementation of userHookService bean, the installing application should create a
 * class(say CustomUserHookService) which implements this interface and finally inject your
 * custom class (CustomUserHookService) as userHookService in resources.groovy.
 *
 * @author Ankit Agrawal
 * @since 0.0.3
 */
interface UserHookService {

    /**
     * This method should contain code which needs to be executed before creating a userInstance during signup.
     */
    void preUserSignup()

    /**
     * This method should contain code which needs to be executed after user is created.
     */
    boolean onCreateUser(User userInstance)

    /**
     * This method should contain code which needs to be executed after user signup is successful.
     */
    void postUserSignup()
}

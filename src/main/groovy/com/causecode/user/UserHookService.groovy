/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

/**
 * This interface declares methods which can be used as hooks during signup.
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
     * This method should contain code which needs to be executed during userSignup.
     */
    boolean onCreateUser(User userInstance)

    /**
     * This method should contain code which needs to be executed after user signup is successful.
     */
    void postUserSignup()
}

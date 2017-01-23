/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import com.causecode.util.NucleusUtils
import grails.transaction.Transactional
import org.pac4j.core.profile.CommonProfile

/**
 * A Service class to manage all User related operations.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
@Transactional
class UserService {

    /**
     * Creates a User from the CommonProfile object after the OAuth process. This method is only called when the User
     * doesn't already exists.
     *
     * @author Nikhil Sharma
     * @since 0.0.1
     */
    void saveOAuthUser(CommonProfile commonProfile) {
        Map dataProperties = [email: commonProfile.email, password: commonProfile.id, username:
                commonProfile.username ?: commonProfile.email, gender: commonProfile.gender?.name().toLowerCase(),
                firstName: commonProfile.firstName, lastName: commonProfile.familyName,
                pictureURL: commonProfile.pictureUrl]

        User userInstance = new User(dataProperties)

        if (NucleusUtils.save(userInstance, true)) {
            log.debug "User saved successfully with email [${userInstance.email}], assigning ROLE_USER to it."

            UserRole userRoleInstance = new UserRole([user: userInstance, role: Role.findByAuthority('ROLE_USER')])
            NucleusUtils.save(userRoleInstance, true)

            OAuthAccount oAuthAccount = new OAuthAccount(user: userInstance, accountId: commonProfile.id)
            oAuthAccount.oAuthProvider = parseOAuthProvider(commonProfile.class.simpleName)
            NucleusUtils.save(oAuthAccount, true)
        } else {
            log.warn "Could not save OAuthUser with email [${commonProfile.email}]"
        }
    }

    /**
     * This method is used to get OAuthProvider from the Profile class name based on the OAuth client.
     *
     * @author Nikhil Sharma
     * @since 0.0.1
     */
    OAuthProvider parseOAuthProvider(String className) {
        OAuthProvider oAuthProvider = OAuthProvider.values().find { OAuthProvider oAuthProvider ->
            className.toLowerCase().contains(oAuthProvider.name().toLowerCase())
        }

        log.info "Found OAuthProvider [$oAuthProvider] for class [$className]"

        return oAuthProvider
    }
}

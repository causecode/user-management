/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.spring.rest

import com.causecode.user.UserService
import grails.core.GrailsApplication
import grails.plugin.springsecurity.rest.oauth.OauthUser
import grails.plugin.springsecurity.rest.oauth.OauthUserDetailsService
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.pac4j.core.profile.CommonProfile
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsChecker
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * This service is used for handling spring rest operations for OAuth users.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
@Slf4j
class CustomOauthUserDetailsService implements OauthUserDetailsService {

    GrailsApplication grailsApplication
    UserDetailsService userDetailsService
    UserService userService
    UserDetailsChecker preAuthenticationChecks

    CustomOauthUserDetailsService() {
        this.grailsApplication = Holders.grailsApplication
        this.userDetailsService = grailsApplication.mainContext.getBean('userDetailsService')
        this.userService = grailsApplication.mainContext.getBean('userService')
        this.preAuthenticationChecks = grailsApplication.mainContext.getBean('preAuthenticationChecks')
    }

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userDetailsService.loadUserByUsername(username)
    }

    @Override
    OauthUser loadUserByUserProfile(CommonProfile userProfile, Collection<GrantedAuthority> defaultRoles)
            throws UsernameNotFoundException {
        UserDetails userDetails
        OauthUser oauthUser

        try {
            log.debug "Trying to fetch user details for user profile: ${userProfile}"
            userDetails = loadUserByUsername(userProfile.email)

            preAuthenticationChecks?.check(userDetails)

            Collection<GrantedAuthority> allRoles = userDetails.authorities + defaultRoles
            oauthUser = new OauthUser(userDetails.username, userDetails.password, allRoles, userProfile)
        } catch (UsernameNotFoundException unfe) {
            log.debug "User not found. Creating a new one with email [${userProfile.email}]"
            userService.saveOAuthUser(userProfile)
            oauthUser = new OauthUser(userProfile.email, 'N/A', defaultRoles, userProfile)
        }

        return oauthUser
    }
}

/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode

import grails.plugins.Plugin
import com.causecode.spring.rest.CustomOauthUserDetailsService
import com.causecode.util.CustomUserDetailsService

/**
 * Contains all information about user-management Plugin
 */
class UserManagementGrailsPlugin extends Plugin {

    def grailsVersion = '3.2.0 > *'
    def pluginExcludes = [
            '**/com/causecode/UrlMappings*/**'
    ]

    def title = 'User Management Plugin' // Headline display name of the plugin
    def author = 'Causecode'
    def authorEmail = ''
    def description = '''\
            Brief summary/description of the plugin.
        '''
    def documentation = 'https://bitbucket.org/causecode/user-management'

    def profiles = ['web-plugin']

    // Changing load order so that injected beans are not overridden by spring-security-core beans.
    def loadAfter = ['spring-security-core']

    /*
     * Note: Few default methods that were not required were removed. Please refer plugin docs if required.
     * Removed methods: doWithApplicationContext, doWithDynamicMethods, onChange, onConfigChange
     * and onShutdown.
     */

    Closure doWithSpring() { { ->
            userDetailsService(CustomUserDetailsService)
            oauthUserDetailsService(CustomOauthUserDetailsService)
        }
    }
}

/*
 * Copyright (c) 2011, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode

import com.causecode.marshaller.UserDomainMarshaller
import grails.converters.JSON

/**
 * A class used for App initialization.
 */
class UserManagementBootStrap {
    def init = { servletContext ->
        log.debug 'User Management Bootstrap started executing ..'

        JSON.registerObjectMarshaller(new UserDomainMarshaller())

        log.debug 'User Management Bootstrap finished executing.'
    }
}

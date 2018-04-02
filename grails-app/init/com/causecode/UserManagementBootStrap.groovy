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

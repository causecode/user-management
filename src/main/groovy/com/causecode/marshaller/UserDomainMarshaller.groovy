/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.marshaller

import com.causecode.user.User
import grails.converters.JSON
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.grails.web.json.JSONWriter

/**
 * Custom marshaller that contains logic to convert input data to JSON format. This class handles all the
 * User Domain related conversions
 *
 * @author Ankit Agrawal
 * @since 0.0.2
 */
@SuppressWarnings(['Instanceof'])
class UserDomainMarshaller implements ObjectMarshaller<JSON> {

    @Override
    boolean supports(Object object) {
        return object instanceof User
    }

    @Override
    void marshalObject(Object object, JSON converter) throws ConverterException {

        User userInstance = object as User
        JSONWriter writer = converter.writer

        writer.object()

        writer.key('id')
        converter.convertAnother(userInstance.id?.toString())

        writer.key('email')
        converter.convertAnother(userInstance.email)

        writer.key('username')
        converter.convertAnother(userInstance.username)

        writer.key('firstName')
        converter.convertAnother(userInstance.firstName)

        writer.key('lastName')
        converter.convertAnother(userInstance.lastName)

        writer.key('gender')
        converter.convertAnother(userInstance.gender)

        writer.key('birthdate')
        converter.convertAnother(userInstance.birthdate)

        writer.key('accountExpired')
        converter.convertAnother(userInstance.accountExpired)

        writer.key('accountLocked')
        converter.convertAnother(userInstance.accountLocked)

        writer.endObject()
    }
}


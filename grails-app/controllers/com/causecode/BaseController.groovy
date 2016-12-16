/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode

import com.causecode.exceptions.InvalidParameterException
import grails.artefact.DomainClass
import org.springframework.http.HttpStatus

/**
 * A trait to act as the BaseController for all controllers. It contains some generic methods and exception handlers
 * that are required by all the controllers in the App.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
trait BaseController {

    static responseFormats = ['json']

    /**
     * Responds map as JSON with success true and status 200.
     *
     * @param data Map to respond as JSON
     *
     * @author Nikhil Sharma
     * @since 0.0.1
     */
    @SuppressWarnings(['Instanceof'])
    boolean respondData(Object objectInstance, Map args = [:]) {

        Map responseData = [:]

        if (args.status && (args.status instanceof HttpStatus)) {
            response.setStatus(args.status.value)
        }

        if (objectInstance instanceof Map) {
            objectInstance.each { k, v ->
                responseData.put(k, v)

                try {
                    if (!v.validate()) {
                        responseData.put(k, v.errors)
                    }
                } catch (MissingMethodException e) {
                    log.warn "$k is not a domain object"
                }
            }
            respond responseData
        } else {
            if ((objectInstance instanceof DomainClass) && !objectInstance.validate()) {
                respond objectInstance.errors
            } else {
                respond objectInstance
            }
        }
    }

    /**
     * This is a Generic handler for the InvalidParameterException thrown from any controller implementing this trait.
     * This responds the error message with response code 422 (UNPROCESSABLE ENTITY)
     *
     * @author Nikhil Sharma
     * @since 0.0.1
     */
    Object handleInvalidParameterException(InvalidParameterException e) {
        log.warn 'InvalidParameterException', e
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value)

        respond([message: e.message])
    }
}

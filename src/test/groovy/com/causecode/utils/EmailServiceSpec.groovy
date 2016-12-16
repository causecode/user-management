/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.utils

import grails.plugin.asyncmail.AsynchronousMailService
import grails.test.mixin.TestFor
import org.slf4j.Logger
import org.springframework.mail.MailSendException

@TestFor(EmailService)
class EmailServiceSpec {

    Object logStatement

    void mockLogger() {
        // Mocking the logger calls to test the log statements.
        service.log = [warn: { Object message, Throwable e = new Exception() ->
            logStatement = message
        } ] as Logger
    }

    void "test sendEmail method when not exception is thrown"() {
        given: 'AsynchronousMailService is mocked to return true'
        service.asynchronousMailService = [sendMail: { Closure closure ->
            return true
        }] as AsynchronousMailService

        when: 'sendEmail method is called with a template closure and eventName'
        boolean result = service.sendEmail({}, 'testEvent')

        then: 'true is returned as no exception is thrown'
        result

        when: 'AsynchronousMailService is mocked to throw exception'
        service.asynchronousMailService = [sendMail: { Closure closure ->
            throw new MailSendException('Test Exception')
        }] as AsynchronousMailService

        result = service.sendEmail({}, 'testEvent')

        then: 'result should be false and logStatement should reflect the message'
        !result
        logStatement == "Error sending email for testEvent"
    }
}

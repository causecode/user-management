/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.utils

import grails.plugin.asyncmail.AsynchronousMailService
import org.springframework.mail.MailException

/**
 * This class is used for sending emails using the AsynchronousMailPlugin. It is a utility class that handles exception
 * while sending emails.
 *
 * @author Nikhil Sharma
 * @since 2.0.0
 */
class EmailService {

    AsynchronousMailService asynchronousMailService

    boolean sendEmail(Closure closure, String eventName) {
        try {
            asynchronousMailService.sendMail(closure)
        } catch (MailException e) {
            log.warn "Error sending email for $eventName", e

            return false
        }

        return true
    }
}

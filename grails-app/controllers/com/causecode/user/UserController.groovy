/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import com.causecode.RestfulController
import com.causecode.exceptions.InvalidParameterException
import com.causecode.util.NucleusUtils
import com.causecode.util.EmailService
import com.causecode.validators.PasswordValidator
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.rest.token.generation.TokenGenerator
import grails.plugin.springsecurity.rest.token.storage.TokenStorageService
import org.springframework.http.HttpStatus

/**
 * An endpoint to handle all requests for User CRUD and other User related operations.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class UserController extends RestfulController {

    static namespace = 'v1'

    SpringSecurityService springSecurityService
    TokenGenerator tokenGenerator
    TokenStorageService tokenStorageService
    EmailService emailService
    GrailsApplication grailsApplication

    UserController() {
        super(User)
    }

    def signUp() {
        Map requestData = request.JSON as Map

        User userInstance = new User()
        userInstance.email = requestData.email
        userInstance.password = requestData.password
        userInstance.username = requestData.username ?: userInstance.email

        if (NucleusUtils.save(userInstance, true)) {
            UserRole userRoleInstance = new UserRole([user: userInstance, role: Role.findByAuthority('ROLE_USER')])
            NucleusUtils.save(userRoleInstance, true)
        } else {
            String message = "Could not save User with email ${userInstance.email}"
            log.warn message
            respondData([message: message], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        String eventName = 'Account Confirmation'
        Closure emailTemplate = {
            to userInstance.email
            subject eventName
            text 'Account successfully created. Use your email address as the username and password.'
            immediate true
        }

        if (!emailService.sendEmail(emailTemplate, eventName)) {
            respondData([message: 'Could not send verification email.'], [status: HttpStatus.EXPECTATION_FAILED])

            return false
        }

        springSecurityService.reauthenticate(userInstance.email)

        AccessToken accessTokenInstance = tokenGenerator.generateAccessToken(springSecurityService.principal)
        tokenStorageService.storeToken(accessTokenInstance.accessToken, springSecurityService.principal)

        respondData(user: userInstance, access_token: accessTokenInstance.accessToken)
    }

    def forgotPassword() {
        Map requestData = request.JSON as Map
        String email = requestData.email

        User userInstance = User.findByUsernameOrEmail(email, email)

        if (!userInstance) {
            throw new InvalidParameterException('No User was found with this email.')
        }

        String token = UUID.randomUUID().toString().replaceAll('-', '')

        AuthenticationToken authenticationToken = AuthenticationToken.findByEmail(email)
        if (authenticationToken) {
            authenticationToken.token = token
        } else {
            authenticationToken = new AuthenticationToken(email: userInstance.email, token: token)
        }

        if (!NucleusUtils.save(authenticationToken, true)) {
            respondData([message: 'Password recovery failed. Please contact support.'])

            return
        }

        String url = grailsApplication.config.grails.passwordRecoveryURL + authenticationToken.token

        String eventName = 'Password Recovery'
        Closure emailTemplate = {
            to userInstance.email
            subject eventName
            text "Follow this link to reset your password ${url}"
            immediate true
        }

        String message = 'Password reset link sent successfully.'
        if (!emailService.sendEmail(emailTemplate, eventName)) {
            message = 'Could not send password recovery link.'
        }

        respondData([message: message])
    }

    def resetPassword() {
        Map requestData = request.JSON as Map

        PasswordValidator resetPasswordValidator = new PasswordValidator(password: requestData.password,
                password2: requestData.password2)

        String token = requestData.token

        AuthenticationToken authenticationToken = AuthenticationToken.findByToken(token)
        if (!authenticationToken) {
            respondData([message: 'Unauthorized User'], [status: HttpStatus.UNAUTHORIZED])

            return
        }

        resetPasswordValidator.email = authenticationToken.email
        resetPasswordValidator.password = requestData.password
        resetPasswordValidator.password2 = requestData.password2
        resetPasswordValidator.validate()

        if (resetPasswordValidator.hasErrors()) {
            String error = resetPasswordValidator.errors.getFieldError('password').code
            respondData([message: error], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return
        }

        User userInstance = User.findByEmail(authenticationToken.email)

        if (!userInstance) {
            log.warn "User not found for token ${authenticationToken}"
            respondData([message: 'User not found.'], [status: HttpStatus.UNAUTHORIZED])

            return
        }

        userInstance.password = resetPasswordValidator.password
        if (!NucleusUtils.save(userInstance, true)) {
            respondData([message: 'Password recovery failed. Please try again or contact support.'])

            return
        }

        authenticationToken.delete(flush: true)

        respondData([message: 'Password changed successfully.'])
    }
}

/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import com.causecode.RestfulController
import com.causecode.SignUpNotAllowedException
import com.causecode.exceptions.InvalidParameterException
import com.causecode.util.NucleusUtils
import com.causecode.validators.PasswordValidator
import grails.core.GrailsApplication
import grails.gsp.PageRenderer
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.rest.token.generation.TokenGenerator
import grails.plugin.springsecurity.rest.token.storage.TokenStorageService
import grails.transaction.Transactional
import org.springframework.http.HttpStatus

/**
 * An endpoint to handle all requests for User CRUD and other User related operations.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
@Secured(['ROLE_ADMIN'])
class UserController extends RestfulController {

    static namespace = 'v1'

    SpringSecurityService springSecurityService
    TokenGenerator tokenGenerator
    TokenStorageService tokenStorageService
    GrailsApplication grailsApplication
    PageRenderer groovyPageRenderer
    UserService userService
    UserHookService userHookService

    UserController() {
        super(User)
    }

    boolean checkIfPermitted(User userInstance) {
        if (userInstance.username != springSecurityService.currentUser.username) {
            if (!SpringSecurityUtils.ifAllGranted('ROLE_ADMIN')) {

                render status: HttpStatus.UNAUTHORIZED.value

                return false
            }
        }

        return true
    }

    @Secured(['permitAll'])
    @Transactional
    def signUp() {
        Map requestData = request.JSON as Map

        try {
            userHookService.preUserSignup()
        } catch (SignUpNotAllowedException e) {
            log.warn 'SignUpNotAllowedException', e
            respondData([message: e.message], [status: HttpStatus.LOCKED])

            return false
        }

        if (!NucleusUtils.validateGoogleReCaptcha(requestData.myRecaptchaResponse)) {
            log.error('Captcha validation failed.')
            respondData([message: 'Captcha Validation Failed'], [status: HttpStatus.EXPECTATION_FAILED])

            return false
        }

        User userInstance = new User()

        bindData(userInstance, requestData)

        if (NucleusUtils.save(userInstance, false)) {
            UserRole userRoleInstance = new UserRole([user: userInstance, role: Role.findByAuthority('ROLE_USER')])
            NucleusUtils.save(userRoleInstance, false, log)
        } else {
            String message = "Could not save User with email ${userInstance.email}"
            log.warn message
            respondData([message: message], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        springSecurityService.reauthenticate(userInstance.email)

        AccessToken accessTokenInstance = tokenGenerator.generateAccessToken(springSecurityService.principal)
        tokenStorageService.storeToken(accessTokenInstance.accessToken, springSecurityService.principal)

        log.debug('Calling onCreateUser hook')

        boolean onCreateHookResponse = userHookService.onCreateUser(userInstance)

        if (!onCreateHookResponse) {
            transactionStatus.setRollbackOnly()
            log.error('User signup failed')
            respondData([message: 'User signup failed, Please contact administrator'],
                    [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        String eventName = 'Account Confirmation'
        Closure emailTemplate = {
            to userInstance.email
            subject eventName
            text 'Account successfully created.'
            immediate true
            beginDate new Date(System.currentTimeMillis() - 2000)
        }

        if (!sendEmail(emailTemplate, eventName)) {
            respondData([message: 'Could not send verification email.'], [status: HttpStatus.EXPECTATION_FAILED])

            return false
        }

        userHookService.postUserSignup()

        respondData(user: userInstance, access_token: accessTokenInstance.accessToken)
    }

    @Secured(['permitAll'])
    def forgotPassword() {
        Map requestData = request.JSON as Map
        String email = requestData.email

        User userInstance = User.findByUsernameOrEmail(email, email)

        if (!userInstance) {
            throw new InvalidParameterException('No User was found with this email.')
        }

        String token = UUID.randomUUID().toString().replaceAll('-', '')

        AuthenticationToken authenticationToken = AuthenticationToken.findByEmail(userInstance.email)
        if (authenticationToken) {
            authenticationToken.token = token
        } else {
            authenticationToken = new AuthenticationToken(email: userInstance.email, token: token)
        }

        if (!save(authenticationToken, true)) {
            log.error('Could not save authentication token')
            respondData([message: 'Password recovery failed. Please contact support.'])

            return false
        }

        String passwordResetLink = userService.passwordResetLink

        if (!passwordResetLink) {
            log.error('PasswordResetLink not configured in the installing App.')
            respondData([message: 'Password recovery failed. Please contact support.'],
                    [status: HttpStatus.EXPECTATION_FAILED])

            return false
        }

        String url = passwordResetLink + authenticationToken.token + '&email=' + authenticationToken.email

        String bodyText = groovyPageRenderer.render([template: '/email-templates/resetPasswordEmail',
                model: [userInstance: userInstance, url: url]])

        String eventName = 'Password Recovery'
        Closure emailTemplate = {
            to userInstance.email
            subject eventName
            html bodyText
            immediate true
            beginDate new Date(System.currentTimeMillis() - 2000)
        }

        String message = 'Password reset link sent successfully.'
        if (!sendEmail(emailTemplate, eventName)) {
            message = 'Could not send password recovery link.'
            response.setStatus(HttpStatus.EXPECTATION_FAILED.value)
        }

        respondData([message: message])
    }

    /**
     * This endpoint verifies that token present in email-link should match token present in authentication_token table.
     *
     * @params String token
     * @return true for valid token and UNAUTHORIZED status for invalid token.
     */
    @Secured(['permitAll'])
    def validatePasswordResetToken() {
        String token = params.token
        String email = params.email

        AuthenticationToken authenticationToken = AuthenticationToken.findByEmailAndToken(email, token)
        if (authenticationToken) {

            return true
        }

        respondData([message: 'Password link has expired. Please generate a new reset link.'],
                [status: HttpStatus.UNAUTHORIZED])
    }

    @Secured(['permitAll'])
    def resetPassword() {
        Map requestData = request.JSON as Map

        PasswordValidator resetPasswordValidator = new PasswordValidator(password: requestData.password,
                password2: requestData.password2)

        String token = requestData.token
        String email = requestData.email

        AuthenticationToken authenticationToken = AuthenticationToken.findByEmailAndToken(email, token)
        if (!authenticationToken) {
            respondData([message: 'Unauthorized User'], [status: HttpStatus.UNAUTHORIZED])

            return false
        }

        resetPasswordValidator.email = authenticationToken.email
        resetPasswordValidator.password = requestData.password
        resetPasswordValidator.password2 = requestData.password2
        resetPasswordValidator.validate()

        if (resetPasswordValidator.hasErrors()) {
            String error = resetPasswordValidator.errors.getFieldError('password').code
            respondData([message: error], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        User userInstance = User.findByEmail(authenticationToken.email)

        if (!userInstance) {
            log.warn "User not found for token ${authenticationToken}"
            respondData([message: 'User not found.'], [status: HttpStatus.UNAUTHORIZED])

            return false
        }

        userInstance.password = resetPasswordValidator.password
        if (!NucleusUtils.save(userInstance, true)) {
            respondData([message: 'Password recovery failed. Please try again or contact support.'])

            return false
        }

        authenticationToken.delete(flush: true)

        respondData([message: 'Password changed successfully.'])
    }

    /*
     * An endpoint to show filtered user details.
     * These details have been filtered and admin-accessible fields have been removed from response.
     * @params userInstance
     * @return objectInstance
     */
    @Secured(['ROLE_USER'])
    def show(User userInstance) {
        if (!checkIfPermitted(userInstance)) {
            return false
        }

        respond(userInstance)
    }

    /*
     * An endpoint to update user details.
     * @params userInstance
     * @return objectInstance
     */
    @Secured(['ROLE_USER'])
    def update() {
        params.putAll(request.JSON as Map)
        User userInstance = User.get(params.id)

        if (!userInstance || !checkIfPermitted(userInstance)) {
            return false
        }

        bindData(userInstance, params, [exclude: ['email', 'username']])

        if (!NucleusUtils.save(userInstance, true, log)) {
            respondData([message: 'Could not update user details'], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        respondData([message: 'Successfully updated user details'])
    }

    /**
     * End point to save user.
     * @params: roleIds - should contain the Ids of the Role that needs to be assigned to the user
     *          dbType - Required (Either 'Mongo' or 'Mysql'
     *          Other params to create User such as email, password, username, firstName, lastName etc.
     * @return User userInstance
     */
    @Override
    save() {
        params.putAll(request.JSON as Map)
        String dbType = params.dbType

        if (!params.roleIds) {
            respondData([message: 'Please provide roleIds in params'], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        if (!(dbType in ['Mysql', 'Mongo'])) {
            respondData([message: 'Please provide a valid dbType param'], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        List<Role> roleList = Role.withCriteria {
            if (dbType == 'Mysql') {
                'in' ('id', params.roleIds)
            } else {
                'in' ('_id', params.roleIds)
            }

            maxResults(50)
        }

        if (!roleList) {
            respondData([message: "Could not find any role with ids ${params.roleIds}"],
                    [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        User userInstance = new User()

        bindData(userInstance, params)

        if (NucleusUtils.save(userInstance, true)) {
            roleList.each { Role roleInstance ->
                UserRole.create(userInstance, roleInstance, true)
            }
        } else {
            respondData([message: "Could not save user instance."], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        respondData(user: userInstance)
    }
}

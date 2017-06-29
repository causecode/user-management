/*
 * Copyright (c) 2017, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import com.causecode.SignUpNotAllowedException
import com.causecode.util.GenericEmailService
import com.causecode.util.NucleusUtils
import com.causecode.validators.PasswordValidatorSpec
import grails.gsp.PageRenderer
import grails.plugin.json.view.JsonViewTemplateEngine
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.rest.token.AccessToken
import grails.plugin.springsecurity.rest.token.generation.TokenGenerator
import grails.plugin.springsecurity.rest.token.storage.TokenStorageService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.runtime.FreshRuntime
import grails.util.Holders
import groovy.json.JsonBuilder
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.mop.ConfineMetaClassChanges

/**
 * This class specifies unit test cases for {@link com.causecode.user.UserController}
 */
@FreshRuntime
@TestFor(UserController)
@Mock([User, Role, UserRole, AuthenticationToken])
class UserControllerSpec extends Specification {

    PageRenderer groovyPageRenderer = Mock(PageRenderer)
    GenericEmailService genericEmailService = Mock(GenericEmailService)

    def doWithSpring = {
        genericEmailService(InstanceFactoryBean, genericEmailService, GenericEmailService)
        groovyPageRenderer(InstanceFactoryBean, groovyPageRenderer, PageRenderer)
    }

    void setup() {
        User.metaClass.encodePassword = {
            return 'password'
        }

        controller.springSecurityService = [reauthenticate: { String email ->
            return email
        } ] as SpringSecurityService

        controller.tokenGenerator = [generateAccessToken: { UserDetails principal ->
            return new AccessToken('random1234')
        } ] as TokenGenerator

        controller.tokenStorageService = [storeToken: { String token, UserDetails principal ->
            return true
        } ] as TokenStorageService

        new Role(authority: 'ROLE_USER').save()
        new Role(authority: 'ROLE_ADMIN').save()
    }

    void mockSendEmail(boolean value) {
        genericEmailService.sendEmail(_, _) >> { Closure closure, String eventName ->
            new JsonBuilder() closure
            return value
        }
    }

    String mockGetPasswordResetLink() {
        controller.userService = Mock(UserService)
        controller.userService.passwordResetLink >> {
            return 'http://localhost:8080/resetPassword'
        }
    }

    @Unroll
    void "test index action for valid JSON response with max #max and offset #offset"() {
        given: 'Some User instances'
        5.times {
            User userInstance = new User(email: "cause-${it + 1}@code.com", password: 'test@123',
                    username: "cause-${it + 1}")
            userInstance.save()
        }

        when: 'Index action is hit'
        controller.params.max = max
        controller.params.offset = offset
        controller.index()

        def result = model.instanceList

        then: 'A valid JSON response should be received'
        controller.response.status == HttpStatus.OK.value
        result.size() == size
        result[0].username == "cause-${offset ? (5 - offset) : 5}"
        view == '/index/index'

        where:
        max | offset | size
        2 | null | 2
        null | 0 | 5
        4 | 1 | 4
        3 | 2 | 3
    }

    void "test index when a view template is defined for user domain"() {
        given: 'Mocked instance of JsonViewTemplateEngine to return true for domain template'
        JsonViewTemplateEngine.metaClass.resolveTemplate = { String template ->
            return true
        }

        when: 'Index action is hit'
        controller.index()

        then: 'Model will have the domain name defined to render domain specific template'
        model.domainName == 'user'
    }

    void "test delete action to return method not allowed"() {
        when: 'delete action is hit'
        controller.delete()

        then: 'Method not allowed status is received'
        response.status == HttpStatus.METHOD_NOT_ALLOWED.value
        response.json.message == 'Method not allowed'
    }

    void "test forgotPassword for failure when user with sent email is not found"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com']

        when: 'The forgotPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'No user found message with 422 status is received'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value
        response.json.message == 'No User was found with this email.'
    }

    void "test forgotPassword for success when user with sent email is found"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com']

        and: 'A User with this email'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        assert userInstance.id

        and: 'Mocked service method call'
        mockGetPasswordResetLink()

        and: 'Mocked sendEmail method'
        mockSendEmail(true)

        when: 'The forgotPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'Password link should be sent'
        response.json.message == 'Password reset link sent successfully.'
        response.status == HttpStatus.OK.value

        when: 'AuthenticationToken already exists for a user'
        controller.response.reset()
        assert AuthenticationToken.findByEmail(userInstance.email).count() == 1
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'Password link should be sent'
        response.json.message == 'Password reset link sent successfully.'
        response.status == HttpStatus.OK.value

        when: 'passwordRecoveryLink is not found'
        controller.userService = Mock(UserService)
        1 * controller.userService.passwordResetLink >> {
            return null
        }
        controller.response.reset()
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'Password reset link won\'t be sent and server responds with appropriate message and status'
        response.json.message == 'Password recovery failed. Please contact support.'
        response.status == HttpStatus.EXPECTATION_FAILED.value()
    }

    void "test forgotPassword for failure when AuthenticationToken is not saved"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com']

        and: 'An User with this email'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        assert userInstance.id

        GroovyMock(NucleusUtils, global: true)
        NucleusUtils.save(_, _) >> {
            return false
        }

        when: 'The forgotPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'Password recovery should fail'
        response.status == HttpStatus.OK.value
        response.json.message == 'Password recovery failed. Please contact support.'
    }

    void "test forgotPassword for failure when reset link is not sent"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com']

        and: 'An User with this email'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        assert userInstance.id

        and: 'Mocked userService method call'
        mockGetPasswordResetLink()

        and: 'Mocked sendEmail method'
        mockSendEmail(false)

        when: 'The forgotPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.forgotPassword()

        then: 'Password link should be not sent'
        response.status == HttpStatus.EXPECTATION_FAILED.value()
        response.json.message == 'Could not send password recovery link.'
    }

    void "test resetPassword when authentication token is incorrect" () {
        given: 'The request parameters'
        Map data = [password: 'test@1234', password2: 'test@1234', token: 'hjagdjhsfgjs33jh43424']

        when: 'The resetPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.resetPassword()

        then: 'Unauthorized user should be received'
        response.json.message == 'Unauthorized User'
        response.status == HttpStatus.UNAUTHORIZED.value
    }

    void "test resetPassword when authentication token is correct" () {
        given: 'An instance of AuthenticationToken and the request parameters'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        Map data = [password: 'Test@1234', password2: 'Test@1234', token: 'hjagdjhsfgjs33jh43424',
                email: userInstance.email]

        assert userInstance.id

        AuthenticationToken authenticationToken = new AuthenticationToken(email: userInstance.email,
                token: 'hjagdjhsfgjs33jh43424')
        authenticationToken.save()

        assert authenticationToken.id

        when: 'The resetPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.resetPassword()

        then: 'Password should successfully reset'
        response.json.message == 'Password changed successfully.'
        response.status == HttpStatus.OK.value
    }

    @Unroll
    void "test resetPassword for validation failure when password is #password" () {
        given: 'An instance of AuthenticationToken and the request parameters'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        Map data = [password: password, password2: password, token: 'hjagdjhsfgjs33jh43424', email: userInstance.email]

        assert userInstance.id

        AuthenticationToken authenticationToken = new AuthenticationToken(email: userInstance.email,
                token: 'hjagdjhsfgjs33jh43424')
        authenticationToken.save()

        assert authenticationToken.id

        when: 'The resetPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.resetPassword()

        then: 'Password validation should fail'
        response.json.message == validationError
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value

        where:
        password                           | validationError
        'cause@code.com'                   | 'Password cannot be the same as the email'
        'test'                             | 'Password length must be at least 8 characters.'
        PasswordValidatorSpec.randomString | 'Password length must not exceed 64 characters.'
        'causecode'                        | 'Password must have at least one letter, number, and special character.'
    }

    void "test resetPassword when user is not found for the authentication token" () {
        given: 'AuthenticationToken instance and the request parameters'
        AuthenticationToken authenticationToken = new AuthenticationToken(email: 'cause@code.com',
                token: 'hjagdjhsfgjs33jh43424')
        authenticationToken.save()

        Map data = [password: 'test@1234', password2: 'test@1234', token: 'hjagdjhsfgjs33jh43424',
                email: 'cause@code.com']

        assert authenticationToken.id

        when: 'The resetPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.resetPassword()

        then: 'User should not be found'
        response.json.message == 'User not found.'
        response.status == HttpStatus.UNAUTHORIZED.value
    }

    @ConfineMetaClassChanges([NucleusUtils])
    void "test resetPassword when user cannot be saved after updation" () {
        given: 'An instance of AuthenticationToken and the request parameters'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()

        Map data = [password: 'test@1234', password2: 'test@1234', token: 'hjagdjhsfgjs33jh43424',
                email: userInstance.email]

        assert userInstance.id

        AuthenticationToken authenticationToken = new AuthenticationToken(email: userInstance.email,
                token: 'hjagdjhsfgjs33jh43424')
        authenticationToken.save()

        assert authenticationToken.id
        assert authenticationToken.toString() == "AuthenticationToken [${userInstance.email}][${data.token}]"

        // Calling it explicitly for testing as this is called only when the object is loaded from database.
        authenticationToken.afterLoad()
        authenticationToken.accessCount == 1

        NucleusUtils.metaClass.static.save = { Object object, boolean flush ->
            return false
        }

        when: 'The resetPassword action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.resetPassword()

        then: 'Password recovery should fail'
        response.json.message == 'Password recovery failed. Please try again or contact support.'
        response.status == HttpStatus.OK.value
    }

    /*
     * Note this test case is for a method and not an action. Not creating another class for a single test case.
     * Also as BaseController is a trait, for testing that we need to create a proxied instance (instance for a
     * trait at runtime). So directly using this controller to test the trait.
     */
    void "test BaseController's respondData method to respond object instance as well as errors"() {
        given: 'An User instance'
        Map userData = [email: 'cause@code.com', password: 'test@123']
        User invalidUserInstance = new User(userData)
        invalidUserInstance.save()

        when: 'RespondData method is called with an User instance with errors'
        controller.respondData(invalidUserInstance)

        then: 'Error object should be responded'
        response.json.errors[0].field == 'username'
        response.json.errors[0].message == 'Property [username] of class [class com.causecode.user.User] cannot be null'

        when: 'RespondData method is called with a valid User instance'
        User userInstance = new User(userData)
        userInstance.username = 'test'
        userInstance.save()

        controller.response.reset()
        controller.respondData(userInstance)

        then: 'Valid json response for the user instance should be received'
        response.json.email == 'cause@code.com'
        response.json.username == 'test'
        response.json.errors == null

        when: 'Object is not a domain class'
        controller.response.reset()
        controller.respondData(['list', 'test'])

        then: 'Object should be responded'
        response.json == ['list', 'test']

        when: 'Object is a map with valid domain instance, invalid domain instance and a non domain object'
        controller.response.reset()
        controller.respondData([user: userInstance, invalidUser: invalidUserInstance, list: ['list', 'test']])

        then: 'Response has key user as valid instance, key invalidUser has errros object and list is as it is'
        response.json.user.email == 'cause@code.com'
        response.json.list == ['list', 'test']
        response.json.invalidUser.errors[0].message == 'Property [username] of class [class com.causecode.user.User] ' +
                'cannot be null'
    }

    void "test update action for various cases"() {
        given: 'Few instances of User'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()
        User userInstance1 = new User(email: 'cause1@code.com', password: 'test1@123', username: 'test1')
        userInstance1.save()
        Map userData = [id: userInstance.id, firstName: 'updatedUsername']

        assert User.count() == 2

        and: 'Mocked SpringSecurityService method'
        controller.springSecurityService = Mock(SpringSecurityService)
        3 * controller.springSecurityService.currentUser >> {
            return userInstance1
        } >> {
            return userInstance
        } >> {
            return userInstance
        }

        GroovyMock(SpringSecurityUtils, global: true)
        1 * SpringSecurityUtils.ifAllGranted(_) >> {
            return false
        }

        GroovyMock(NucleusUtils, global: true)
        2 * NucleusUtils.save(_, _, _) >> {
            userInstance.firstName = 'updatedUserName'
            userInstance.save(flush: true)

            return true
        } >> {
            return false
        }

        when: 'update action is hit and user is not authorised to edit the instance'
        controller.request.method = 'GET'
        controller.request.json = userData
        controller.update()

        then: 'Server responds with Unauthorised status code and instance is not updated'
        controller.response.status == HttpStatus.UNAUTHORIZED.value()
        userInstance.refresh().username == 'test'

        when: 'update action is hit and user is authorised to edit the instance'
        controller.response.reset()
        controller.request.json = userData
        controller.update()

        then: 'Instance is updated'
        controller.response.status == 200
        userInstance.refresh().firstName == 'updatedUserName'
        controller.response.json.message == 'Successfully updated user details'

        when: 'update action is hit and user is authorised to edit the instance but fields contain errors'
        controller.response.reset()
        controller.request.json = [username: null]
        controller.update()

        then: 'Instance is not updated'
        controller.response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
        controller.response.json.message == 'Could not update user details'
    }

    void "test show action for various cases"() {
        given: 'User instances'
        User userInstance = new User(email: 'cause@code.com', password: 'test@123', username: 'test')
        userInstance.save()
        User userInstance1 = new User(email: 'cause1@code.com', password: 'test1@123', username: 'test1')
        userInstance1.save()

        assert User.count() == 2

        and: 'Mocked SpringSecurity methods'
        controller.springSecurityService = Mock(SpringSecurityService)
        2 * controller.springSecurityService.currentUser >> {
            return userInstance1
        } >> {
            return userInstance
        }

        GroovyMock(SpringSecurityUtils, global: true)
        1 * SpringSecurityUtils.ifAllGranted(_) >> {
            return false
        }

        when: 'show action is hit and user is not authorised to see the instance'
        controller.request.method = 'GET'
        controller.params.id = userInstance.id
        controller.show()

        then: 'Server responds with Unauthorised status code'
        controller.response.status == HttpStatus.UNAUTHORIZED.value()

        when: 'show action is hit and user is authorised to see the instance'
        controller.response.reset()
        controller.request.method = 'GET'
        controller.params.id = userInstance.id
        controller.show()

        then: 'Server responds with user accessible data'
        controller.response.status == 200
        noExceptionThrown()
        controller.response.json.id == userInstance.id
        controller.response.json.email == userInstance.email
        controller.response.json.username == userInstance.username
    }

    void "test user signup when exception occurs while sending email"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com', password: 'test@1234', username: 'test']

        and: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            return
        }
        1 * controller.userHookService.onCreateUser(_) >> {
            return true
        }

        assert User.count() == 0

        and: 'Mocked NucleusUtils method call for captchaValidation'
        GroovyMock(NucleusUtils, global: true)
        1 * NucleusUtils.validateGoogleReCaptcha(_) >> {
            return true
        }
        1 * NucleusUtils.save(_, _) >> {
            return true
        }

        and: 'Mocked sendEmail method'
        mockSendEmail(false)

        when: 'The signup action is hit with above parameters and mail could not be sent'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.signUp()

        then: 'Server responds with error message'
        response.json.message == 'Could not send verification email.'
        response.status == HttpStatus.EXPECTATION_FAILED.value
    }

    void "test user signup for success when email, password and username parameters are correct"() {
        given: 'The request parameters'
        Map data = [email: 'cause@code.com', password: 'test@1234', username: 'test']

        assert User.count() == 0

        and: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            return
        }
        1 * controller.userHookService.onCreateUser(_) >> {
            return true
        }
        1 * controller.userHookService.postUserSignup() >> {
            return
        }

        and: 'Mocked NucleusUtils method call for captcha validation'
        GroovyMock(NucleusUtils, global: true)
        1 * NucleusUtils.validateGoogleReCaptcha(_) >> {
            return true
        }
        1 * NucleusUtils.save(_, _) >> {
            return true
        }

        and: 'Mocked sendEmail method'
        mockSendEmail(true)

        when: 'The signup action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.signUp()

        then: 'A User is successfully created and access_token is generated for that user'
        response.status == HttpStatus.OK.value
        controller.response.json.access_token == 'random1234'
    }

    @Unroll
    void "test user signup for failure when email is #email, password is #password and username is #username"() {
        given: 'The request parameters'
        Map data = [email: email, password: password, username: username]

        and: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            return
        }

        and: 'Mocked NucleusUtils method call for captcha validation'
        GroovyMock(NucleusUtils, global: true)
        1 * NucleusUtils.validateGoogleReCaptcha(_) >> {
            return true
        }

        1 * NucleusUtils.save(_, _) >> {
            return false
        }

        when: 'The signup action is hit with above parameters'
        controller.request.method = 'POST'
        controller.request.json = data
        controller.signUp()

        then: 'User should not be created'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value
        response.json.message == message

        where:
        email            | password    | username | message
        null             | null        | null     | 'Could not save User with email null'
        'cause@code.com' | null        | 'test'   | 'Could not save User with email cause@code.com'
        null             | 'test@1234' | ''       | 'Could not save User with email null'
        'test'           | ''          | ''       | 'Could not save User with email test'
    }

    void "test validatePasswordResetToken for various cases"() {
        given: 'An instance of AuthenticationToken'
        AuthenticationToken authenticationToken = new AuthenticationToken(email: 'test@causecode.com',
                token: 'testTokenString')
        authenticationToken.save(flush: true)

        assert authenticationToken.id

        when: 'validatePasswordResetToken action is hit and token is invalid'
        controller.request.method = 'GET'
        controller.params.email = 'test@causecode.com'
        controller.params.token = 'invalidToken'
        controller.validatePasswordResetToken()

        then: 'Server responds with error code and appropriate message'
        controller.response.status == HttpStatus.UNAUTHORIZED.value()
        controller.response.json.message == 'Password link has expired. Please generate a new reset link.'

        when: 'validatePasswordResetToken action is hit and token is valid'
        controller.response.reset()
        controller.request.method = 'GET'
        controller.params.token = 'testTokenString'
        controller.params.email = 'test@causecode.com'
        controller.validatePasswordResetToken()

        then: 'Server responds true'
        controller.response.status == HttpStatus.OK.value()
    }

    void "test signup action when captcha validation fails"() {
        given: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            return
        }

        when: 'signUp action is hit and captcha validation fails'
        controller.request.method = 'POST'
        controller.request.json = [myRecaptchaResponse: 'testCaptchaResponse']
        controller.signUp()

        then: 'Server responds with appropriate status and message'
        controller.response.status == HttpStatus.EXPECTATION_FAILED.value()
        controller.response.json.message == 'Captcha Validation Failed'
    }

    void "test signUp action when error occurs for any of the hooks"() {
        given: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            return
        }
        1 * controller.userHookService.onCreateUser(_) >> {
            return false
        }

        and: 'Mocked NucleusUtils method call for captcha validation'
        GroovyMock(NucleusUtils, global: true)
        1 * NucleusUtils.validateGoogleReCaptcha(_) >> {
            return true
        }
        1 * NucleusUtils.save(_, _) >> {
            return true
        }

        when: 'signUp action is hit and error occures in onCreateUser hook'
        controller.request.method = 'POST'
        controller.request.json = [myRecaptchaResponse: 'testCaptchaResponse']
        controller.signUp()

        then: 'Server reponds with appropriate message and status code'
        response.json.message == 'User signup failed, Please contact administrator'
        response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }

    void "test signUp action when signUp is disabled in the installing application"() {
        given: 'Mocked DefaultUserHookService methods'
        controller.userHookService = Mock(DefaultUserHookService)
        1 * controller.userHookService.preUserSignup() >> {
            throw new SignUpNotAllowedException('Signup has been disabled')
        }

        when: 'signUp action is hit and error occures in onCreateUser hook'
        controller.request.method = 'POST'
        controller.request.json = [myRecaptchaResponse: 'testCaptchaResponse']
        controller.signUp()

        then: 'Server reponds with appropriate message and status code'
        response.json.message == 'Signup has been disabled'
        response.status == HttpStatus.LOCKED.value()
    }

    @Unroll
    void "test save endpoint when invalid data is passed in request"() {
        given: 'A few instance of Role'
        new Role(authority: 'ROLE_USER').save(flush: true)
        new Role(authority: 'ROLE_ADMIN').save(flush: true)
        new Role(authority: 'ROLE_MANAGER').save(flush: true)

        when: 'save endpoint is hit and invalid params are passed'
        Holders.config.dataSource.driverClassName = mysqlDriver
        Holders.config.dataSource.url = mysqlUrl
        Holders.config.grails.mongodb.databaseName = mongoDBName
        Holders.config.grails.mongodb.host = mongoHost

        controller.request.method = 'POST'
        controller.params.roleIds = roleIds
        controller.params.email = null
        controller.save()

        then: 'Server responds with valid error message and code'
        controller.response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
        controller.response.json.message == responseMessage

        where:
        roleIds          | mysqlDriver   |  mysqlUrl     | mongoDBName     | mongoHost    | responseMessage
        []               | 'com.mysql.'  | 'jdbc:mysql:' | null            | null         |
                'Please provide roleIds in params'
        [10234, 984357]  | ''            | ''            | null            | null         |
                'Could not infer dbType from application config.'
        [10234, 984357]  | 'com.mysql.'  | 'jdbc:mysql:' | null            | null         |
                'Could not find any role with ids [10234, 984357]'
        [1]              | 'com.mysql.'  | 'jdbc:mysql:' | null            | null         |
                'Could not save user instance.'
        [10234, 984357]  | 'com.mysql'   | 'jdbc:mysql:' | 'test_mongo'    | 'localhost'  |
                'Could not infer dbType from application config.'
    }

    void "test save endpoint when valid data is passed to create User"() {
        given: 'A few instance of Role'
        Role roleInstance = new Role(authority: 'ROLE_DUMMY_USER').save(flush: true)
        Role roleInstance1 = new Role(authority: 'ROLE_DUMMY_ADMIN').save(flush: true)
        Role roleInstance2 = new Role(authority: 'ROLE_DUMMY_MANAGER').save(flush: true)
        Holders.config.dataSource.driverClassName = 'com.mysql.jdbc.driver'
        Holders.config.dataSource.url = 'jdbc:mysql://localhost:3306/test'

        when: 'save endpoint is hit and valid params are passed'
        controller.request.method = 'POST'
        controller.params.roleIds = [roleInstance.id, roleInstance1.id, roleInstance2.id]
        controller.params.email = 'testuser@causecode.com'
        controller.params.username = 'test user'
        controller.params.password = 'causecode.11'
        controller.save()

        then: 'Server responds with valid User instance'
        controller.response.status == HttpStatus.OK.value()
        User userInstance = User.get(controller.response.json.user.id)
        userInstance.id != null
        userInstance.email == 'testuser@causecode.com'
        UserRole.findAllByUser(userInstance).size() == 3
    }

    void "test fetchRoles action to get List of Roles"() {
        given: 'A few instance of Roles'
        new Role(authority: 'ROLE_MANAGER').save(flush: true)
        new Role(authority: 'ROLE_USER_MANAGER').save(flush: true)

        when: 'fetchRoles endpoint is hit'
        controller.request.method = 'GET'
        controller.fetchRoles()

        then: 'Roles are responded in response'
        controller.response.json.roles.size() == 4 // 2 were created in setup block.
    }
}

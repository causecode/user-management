/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.Gender
import org.pac4j.oauth.profile.facebook.FacebookProfile
import org.pac4j.oauth.profile.google2.Google2Profile
import org.pac4j.oauth.profile.linkedin2.LinkedIn2Profile
import org.pac4j.oauth.profile.twitter.TwitterProfile
import spock.lang.Specification
import spock.lang.Unroll

/**
 * This class specifies unit test cases for {@link com.causecode.user.UserService}
 */
@TestFor(UserService)
@Mock([User, UserRole, Role, OAuthAccount])
class UserServiceSpec extends Specification {

    void setupSpec() {
        User.metaClass.encodePassword = {
            return 'password'
        }
    }

    CommonProfile getCommonProfileObject() {
        Map dataMap = [
            id: 123456789,
            attributes: [
                 'name.familyName': 'Doe',
                 email: 'john.doe@test.com',
                 access_token: 'sdfasdfasdffasdfasdfasdfasdfasfs.dfasdfasd435234afsasdf',
                 gender: Gender.MALE,
                 displayName: 'John Doe',
                 'name.givenName': 'John',
                 language: 'en_GB',
                 'image.url': 'https://picturUrl.com/picture.jpg',
                 url: 'https://profile-link/123456789'
            ],
        ]

        CommonProfile commonProfile = new TwitterProfile()
        commonProfile.addAttributes(dataMap.attributes)
        commonProfile.id = dataMap.id

        return commonProfile
    }

    void "test findAllByAuthority method to return matching UserRoles"() {
        when: 'findAllByAuthority method is called and no matching users are found'
        List users = service.findAllByAuthority(['ROLE_USER'])

        then: 'Method returns empty list'
        users == []
    }

    void "test createOAuthUser method for creating a User instance from CommonProfile object"() {
        given: 'An instance of CommonProfile'
        CommonProfile commonProfile = commonProfileObject

        Role.findOrCreateByAuthority('ROLE_USER').save()

        when: 'The saveOAuthUser method is called'
        service.saveOAuthUser(commonProfile)

        then: 'User and corresponding OAuthAccount should be successfully created'
        User.count() == 1
        User userInstance = User.first()
        userInstance.email == commonProfile.email

        OAuthAccount.count() == 1
        OAuthAccount oAuthAccountInstance = OAuthAccount.first()
        oAuthAccountInstance.user == userInstance
        oAuthAccountInstance.oAuthProvider == OAuthProvider.TWITTER
        oAuthAccountInstance.toString() == "OAuthAccount ($userInstance)($oAuthAccountInstance.oAuthProvider)"
    }

    void "test createOAuthUser when user already exists with same email"() {
        given: 'An instance of CommonProfile'
        CommonProfile commonProfile = commonProfileObject
        Role.findOrCreateByAuthority('ROLE_USER').save()

        and: 'An User with same email'
        User userInstance = new User(email: commonProfile.email, password: 'test@123', username: 'test')
        userInstance.save()
        assert userInstance.id

        assert User.count() == 1

        when: 'The saveOAuthUser method is called'
        service.saveOAuthUser(commonProfile)

        then: 'User should not be saved'
        User.count() == 1
    }

    @Unroll
    void "test parseOAuthProvider to parse correct provider when provider is #provider"() {
        when: 'The parseOAuthProvider is called with the provider class name'
        OAuthProvider oAuthProvider = service.parseOAuthProvider(providerClassName)

        then: 'OAuthProvider should match with the profile'
        oAuthProvider.name() == oAuthProviderName

        where:
        providerClassName           | oAuthProviderName
        Google2Profile.simpleName   | 'GOOGLE'
        LinkedIn2Profile.simpleName | 'LINKEDIN'
        TwitterProfile.simpleName   | 'TWITTER'
        FacebookProfile.simpleName  | 'FACEBOOK'
    }

    void "test getPasswordResetLink method to return resetPasswordLink"() {
        given: 'Grials configurations'
        grailsApplication.config.grails.passwordRecoveryURL = 'http://test.com'

        expect:
        service.passwordResetLink == 'http://test.com'
    }
}

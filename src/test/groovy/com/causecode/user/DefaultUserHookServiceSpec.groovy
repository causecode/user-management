package com.causecode.user

import spock.lang.Specification

/**
 * This class specifies unit test cases for {@link com.causecode.user.DefaultUserHookService}
 */
class DefaultUserHookServiceSpec extends Specification {

    void "test various methods in DefaultUserHookService class"() {
        given: 'An instance of DefaultUserHookService class'
        DefaultUserHookService defaultUserHookService = new DefaultUserHookService()

        when: 'preUserSignup method is called'
        def response = defaultUserHookService.preUserSignup()

        then: 'No exception is thrown and method returns null'
        noExceptionThrown()
        response == null

        when: 'onCreateUser method is called'
        response = defaultUserHookService.onCreateUser(null)

        then: 'Method should return false'
        !response

        when: 'postUserSignup method is called'
        response = defaultUserHookService.postUserSignup()

        then: 'Method returns null and no Exceptions are thrown'
        response == null
        noExceptionThrown()
    }
}

package com.causecode

import com.causecode.user.AuthenticationToken

/**
 * This Job is used for cleaning the Authentication tokens. This job run at 3am daily.
 *
 * @author Nikhil Sharma
 * @since 0.0.1
 */
class AuthenticationTokenCleanUpJob {

    static triggers = {
        // Starts with one hour delay and repeats at 3:00 am.
        cron startDelay: 3600000l, cronExpression: '0 0 3 * * ? *'
    }

    def execute() {
        log.debug 'Started executing AuthenticationTokenCleanUpJob...'

        AuthenticationToken.withCriteria {
            lt('lastUpdated', new Date() - 60)

            maxResults(1000)
        }*.delete(flush: true)

        log.debug 'Finished executing AuthenticationTokenCleanUpJob...'
    }
}

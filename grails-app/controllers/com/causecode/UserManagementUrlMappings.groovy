package com.causecode

import grails.util.Environment

/**
 * This class is used for mapping requests to controller and actions.
 */
class UserManagementUrlMappings {

    static mappings = {

        '/user/signUp'(controller: 'user', action: 'signUp')
        '/user/forgotPassword'(controller: 'user', action: 'forgotPassword')
        '/user/resetPassword'(controller: 'user', action: 'resetPassword')

        if (Environment.current != Environment.PRODUCTION) {
            '/dummy'(controller: 'dummy', action: 'test')
        }

        '500'(view: '/error')
        '404'(view: '/notFound')
    }
}

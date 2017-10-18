/*
 * Copyright (c) 2011-Present CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.runtime.DirtiesRuntime
import grails.util.Holders
import org.springframework.http.HttpStatus
import spock.lang.Specification

import java.sql.SQLException

/**
 * This class specifies unit test cases for {@link com.causecode.user.UserManagementController}
 */
@TestFor(UserManagementController)
@Mock([User, Role, UserRole])
@SuppressWarnings(['MethodCount']) // added to suppress the codenarc voilation of exceeding 30 methods in a class.
class UserManagementControllerSpec extends Specification {

    User adminUser, managerUser, normalUser, trialUser
    Role adminRole, userManagerRole, userRole

    void mockSpringSecurityUtils(boolean returnValue) {
        SpringSecurityUtils.metaClass.'static'.ifAnyGranted = { String role ->
            return returnValue
        }
    }

    void assignRolesToUsers(Role roleOne, Role roleTwo, Role roleThree) {
        UserRole.create(adminUser, roleOne, true)
        UserRole.create(normalUser, roleTwo, true)
        UserRole.create(trialUser, roleThree, true)
    }

    void mockUserManagementService () {
        // In request, 3 userIds are passed so countUserIds is initialised to 3
        // In request, 1 roleId is passed so countRoleIds is initialised to 1
        int countUserIds = 3
        int countRoleIds = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (countUserIds == 3 && ids.size() == 3) {
                return [adminUser.id, trialUser.id, normalUser.id]
            }
            if (countRoleIds == 1 && ids.size() == 1) {
                return [userRole.id]
            }
        } ] as UserManagementService
    }

    def setup() {
        adminUser = new User([username: 'admin', password: 'admin@13', email: 'adminbootstrap@causecode.com',
                firstName: 'CauseCode', lastName: 'Technologies', gender: 'male', accountLocked: false
        ])
        SpringSecurityService springSecurityServiceForAdminUser = new SpringSecurityService()
        springSecurityServiceForAdminUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        adminUser.springSecurityService = springSecurityServiceForAdminUser
        assert adminUser.save(flush: true)

        managerUser = new User([username: 'manager', password: 'manager@13', email: 'managerbootstrap@causecode.com',
                firstName: 'CauseCode', lastName: 'Technologies', gender: 'male', accountLocked: false
        ])
        SpringSecurityService springSecurityServiceForManagerUser = new SpringSecurityService()
        springSecurityServiceForManagerUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        managerUser.springSecurityService = springSecurityServiceForManagerUser
        assert managerUser.save(failOnError: true, flush: true)

        normalUser = new User([username: 'normal', password: 'normal@13', email: 'normalbootstrap@causecode.com',
                firstName: 'normalCauseCode', lastName: 'normalTechnologies', gender: 'male', accountLocked: false
        ])
        SpringSecurityService springSecurityServiceForNormalUser = new SpringSecurityService()
        springSecurityServiceForNormalUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        normalUser.springSecurityService = springSecurityServiceForNormalUser
        assert normalUser.save(flush: true)

        trialUser = new User([username: 'trial', password: 'trial@13', email: 'trailbootstrap@causecode.com',
                firstName: 'trialCauseCode', lastName: 'trialTechnologies', gender: 'male', accountLocked: false
        ])
        SpringSecurityService springSecurityServiceTrial = new SpringSecurityService()
        springSecurityServiceTrial.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        trialUser.springSecurityService = springSecurityServiceTrial
        assert trialUser.save(flush: true)

        adminRole = Role.findOrSaveByAuthority('ROLE_ADMIN')
        assert adminRole.save(flush: true)
        userManagerRole = Role.findOrSaveByAuthority('ROLE_USER_MANAGER')
        assert userManagerRole.save(flush: true)
        userRole = Role.findOrSaveByAuthority('ROLE_USER')
        assert userRole.save(flush: true)
    }

    void "test index action"() {
        given:'User instance list and mocked UserManagementService'
        List<User> userInstanceList = [adminUser, normalUser]
        controller.userManagementService = [listForMysql: { Map params ->
            return [instanceList: userInstanceList, totalCount: userInstanceList.size()]
        } ] as UserManagementService
        Holders.config.dataSource.driverClassName = 'com.mysql'
        Holders.config.dataSource.url = 'jdbc:mysql:'

        when: 'index action is hit'
        controller.index()

        then: 'default values will be set in params'
        controller.params.offset == 0
        controller.params.max == 10
        controller.params.order == 'desc'
        controller.response.json.totalCount == 2

        when: 'index action is hit'
        controller.params.max = 100
        controller.params.offset = 10
        controller.index()

        then: 'provided values will be set in params'
        controller.params.offset == 10
        controller.params.max == 100
        controller.response.json.totalCount == 2
    }

    void "test Index action with date filter applied"() {
        given: 'User instance list and mocked UserManagementService'
        List<User> userInstanceList = [adminUser]
        controller.userManagementService = [listForMysql: { Map params ->
            return [instanceList: userInstanceList, totalCount: userInstanceList.size()]
        } ] as UserManagementService

        when: 'Index action is hit'
        Holders.config.dataSource.driverClassName = 'com.mysql.jdbc.driver'
        Holders.config.dataSource.url = 'jdbc:mysql://localhost:3306/test'
        controller.params.max = 15
        controller.params.offset = 0
        controller.index()

        then: 'List of users will be returned'
        response.json['instanceList'] != null
        response.json.instanceList[0].id
        response.json['totalCount'] != null
        response.json.totalCount == 1
        response.json['roleList'] != null
    }

    void "test index action when no database is inferred from application config"() {
        given: 'mock config properties'
        Holders.config.dataSource.driverClassName = ''
        Holders.config.dataSource.url = ''

        when: 'index method is hit with invalid config params'
        controller.params.max = 100
        controller.params.offset = 10
        controller.index()

        then: 'valid error message must be received in response'
        response.json['message'] == 'Could not infer dbType from application config.'
    }

    @DirtiesRuntime
    void "test if an ADMIN user tries to Modify role of another ADMIN user"() {
        given: 'ADMIN user logged-in for role modification'
        UserRole.create(managerUser, adminRole, true)
        controller.request.json = [userIds: [managerUser.id], roleIds: [adminRole.id, userManagerRole.id, userRole.id],
                roleActionType: 'refresh'
        ] as JSON
        controller.request.method = 'POST'
        mockSpringSecurityUtils(true)
        int index = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (index == 1) {
                index++
                return [managerUser.id]
            }
            return [adminRole.id]
        } ] as UserManagementService

        when: 'Admin tries to modify role of other ADMIN in Refresh mode'
        controller.modifyRoles()

        then: 'He should be allowed to do so in virtue'
        controller.response.status == 200       // Successful execution of the action
        Set<Role> adminAuthorities = managerUser.authorities
        adminRole in adminAuthorities
        // Previous roles are wiped off
        userManagerRole in adminAuthorities == false
        userRole in adminAuthorities == false
    }

    @DirtiesRuntime
    void "test if a ADMIN user modifies another users role without refresh mode"() {
        given: 'Request for Role modification without Refresh mode'
        mockSpringSecurityUtils(true)
        controller.request.json = [roleActionType: '', userIds: [normalUser.id],
                roleIds: [userManagerRole.id, userRole.id]
        ]
        controller.request.method = 'POST'
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [normalUser.id, userManagerRole.id]
        } ] as UserManagementService
        Set<Role> normalUserRoles = normalUser.authorities
        assert !(userManagerRole in normalUserRoles)
        assert !(userRole in normalUserRoles)

        when: 'Admin select normal user and decides him to assign usermanager role as well'
        controller.modifyRoles()

        then: 'Now, Normal user must have roles of both normaluser and usermanager'
        Set<Role> normalUserAuthorities = normalUser.authorities
        userManagerRole in normalUserAuthorities
        userRole in normalUserAuthorities
    }

    @DirtiesRuntime
    void "test if a Non-ADMIN user is trying to Modify other user's roles"() {
        given: 'Request for Role modification in Refresh mode'
        mockSpringSecurityUtils(false)
        UserRole.create(adminUser, adminRole, true) // assigning admin role to admin user
        UserRole.create(normalUser, userManagerRole, true)  // assigning userManagerRole to normalUser
        UserRole.create(trialUser, userManagerRole, true) // assigning userManagerRole to trialUser

        controller.request.json = [roleActionType: 'refresh', userIds: [adminUser.id, trialUser.id, normalUser.id],
                roleIds: [userRole.id]
        ]
        controller.request.method = 'POST'
        mockUserManagementService()

        when: 'Logged-in user selects 1 ADMIN and 2 normal users for role modification'
        controller.modifyRoles()

        then: 'Admin users must be removed from the ID list and Normal User\'s roles should be updated'
        Set<Role> adminAuthorities = adminUser.authorities

        adminRole in adminAuthorities
        userRole in adminAuthorities == false
        userManagerRole in adminAuthorities == false

        Set<Role> trialUserAuthorities = trialUser.authorities
        userRole in trialUserAuthorities
        userManagerRole in trialUserAuthorities == false

        Set<Role> normalUserAuthorities = normalUser.authorities
        userRole in normalUserAuthorities
        userManagerRole in normalUserAuthorities == false
    }

    @DirtiesRuntime
    void "test if a Non-ADMIN user selects all admin users to modify role"() {
        given:'Mocking SpringSecurityUtils'
        mockSpringSecurityUtils(false)
        UserRole.create(adminUser, adminRole, true) // assigning admin role to admin user
        UserRole.create(normalUser, adminRole, true)  // assigning adminRole to normalUser
        UserRole.create(trialUser, adminRole, true) // assigning adminRole to trialUser

        controller.request.json = [roleActionType: 'refresh', userIds: [adminUser.id, trialUser.id, normalUser.id],
                roleIds: [userRole.id]
        ]
        controller.request.method = 'POST'
        mockUserManagementService()

        when: 'Logged-in user selects 3 ADMIN for role modification'
        controller.modifyRoles()

        then: 'All admin users must be removed from the list and status code 406 must be returned'
        controller.response.status == HttpStatus.NOT_ACCEPTABLE.value()
        controller.response.json.success == false
        controller.response.json.message.contains('Please select at least one user.Users with role Admin are ' +
                'excluded from selected list.')
    }

    @DirtiesRuntime
    void "test if a Non-ADMIN user tries to assign admin role to any user"() {
        given:'Mocking SpringSecurityUtils'
        mockSpringSecurityUtils(false)
        assignRolesToUsers(adminRole, userRole, userRole)
        controller.request.json = [roleActionType: 'refresh', userIds: [adminUser.id, trialUser.id, normalUser.id],
                roleIds: [adminRole.id]
        ]
        controller.request.method = 'POST'
        // In request, 3 userIds are passed so countUserIds is initialised to 3
        // In request, 1 roleId is passed so countRoleIds is initialised to 1
        int countUserIds = 3
        int countRoleIds = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (countUserIds == 3 && ids.size() == 3) {
                return [adminUser.id, trialUser.id, normalUser.id]
            }
            if (countRoleIds == 1 && ids.size() == 1) {
                return [adminRole.id]
            }
        } ] as UserManagementService

        when: 'Logged-in user selects 3 ADMIN for role modification'
        controller.modifyRoles()

        then: 'All admin users must be removed from the list and status code 406 must be returned'
        controller.response.status == HttpStatus.NOT_ACCEPTABLE.value()
        controller.response.json.success == false
        controller.response.json.message.contains('No Roles selected.Only Users with Admin role can assign ' +
                'Admin roles.')
    }

    @DirtiesRuntime
    void "test if modification of roles fail due to invalid instance"() {
        given:'Mocking SpringSecurityUtils'
        mockSpringSecurityUtils(true)
        assignRolesToUsers(adminRole, userRole, userRole)

        controller.request.json = [roleActionType: 'refresh', userIds: [adminUser.id, trialUser.id, normalUser.id],
                roleIds: [adminRole.id]
        ]
        controller.request.method = 'POST'

        UserRole.metaClass.'static'.create = { User user, Role role, boolean flush = false ->
            return null
        }

        mockUserManagementService()

        when: 'Logged-in user selects 3 ADMIN for role modification'
        controller.modifyRoles()

        then: 'All admin users must be removed from the list and status code 406 must be returned'
        controller.response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
        controller.response.json.success == false
        controller.response.json.message.contains('Unable to grant role for users with email(s)')
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() to see if a NON-ADMIN user is trying to change status of an ADMIN user"() {
        given: 'Deactivation request for 2 non-admin and 1 Admin user'
        mockSpringSecurityUtils(false)
        assignRolesToUsers(adminRole, userRole, userRole)

        controller.request.json = [selectedIds: [adminUser.id, trialUser.id, normalUser.id], lockAccount: true]
        controller.request.method = 'POST'

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [adminUser.id, trialUser.id, normalUser.id]
        } ] as UserManagementService

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Admin users must be removed from the current ID list'
        // 'refresh' action will fetch the updated values from the database
        adminUser.refresh().accountLocked == false   // ADMIN user Not deactivated
        trialUser.refresh().accountLocked == true
        normalUser.refresh().accountLocked == true
    }

    @DirtiesRuntime
    void "test makeUserInactive() when non-admin user tries to change active status of all admin users"() {
        given: 'Deactivation request for 3 admin users'
        mockSpringSecurityUtils(false)
        UserRole.create(adminUser, adminRole, true) // assigning admin role to admin user
        UserRole.create(normalUser, adminRole, true)  // assigning adminRole to adminUser2
        UserRole.create(trialUser, adminRole, true) // assigning adminRole to adminUser3

        controller.request.json = [selectedIds: [adminUser.id, normalUser.id, trialUser.id], lockAccount: true]
        controller.request.method = 'POST'

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [adminUser.id, normalUser.id, trialUser.id]
        } ] as UserManagementService

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Admin users must be removed from the current ID list'
        // 'refresh' action will fetch the updated values from the database
        adminUser.refresh().accountLocked == false   // ADMIN user Not deactivated
        normalUser.refresh().accountLocked == false
        trialUser.refresh().accountLocked == false
        controller.response.json.success == false
        controller.response.json.message.contains('Please select at least one user.Users with role Admin are ' +
                'excluded from selected list.')
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() if ADMIN user changes activation status of other ADMIN User"() {
        given: 'Admin user to perform Role modification'
        // USER_MANAGER with Admin role is currently logged in
        mockSpringSecurityUtils(true)
        assignRolesToUsers(adminRole, userRole, userRole)

        and: 'Request with Admin and Normal user Ids for deactivation'
        controller.request.json = [selectedIds: [adminUser.id, trialUser.id, normalUser.id], lockAccount: true]
        controller.request.method = 'POST'

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [adminUser.id, trialUser.id, normalUser.id]
        } ] as UserManagementService

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'De-activation is allowed on all Users'
        adminUser.refresh().accountLocked == true
        trialUser.refresh().accountLocked == true
        normalUser.refresh().accountLocked == true
        controller.response.json.success == true
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() when no user Id is selected"() {
        given: 'Blank user id list or Admin Ids removed'
        controller.request.json = [selectedIds: [], lockAccount: true]

        mockSpringSecurityUtils(true)
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return []
        } ] as UserManagementService

        controller.request.method = 'POST'

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Action should be aborted and error message should be sent'
        controller.response.json.success == false
        controller.response.json.message.contains('Please select at least one user.')
    }

    @DirtiesRuntime
    void "test if selectedUserId list is blank in modifyRoles action"() {
        when: 'Selected Ids are empty or Id is removed because of ADMIN permission'
        mockSpringSecurityUtils(true)
        Role userManagerRole = Role.findOrSaveByAuthority('ROLE_USER_MANAGER')
        assert userManagerRole.save(flush: true)
        Role adminRole = Role.findOrSaveByAuthority('ROLE_ADMIN')
        assert adminRole.save(flush: true)

        controller.request.method = 'POST'
        controller.request.json = [userIds: [], roleIds: [userManagerRole.id, adminRole.id]]

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return
        } ] as UserManagementService

        and: 'Modify action is hit'
        controller.modifyRoles()

        then: 'Error message is thrown'
        controller.response.json.success == false
        controller.response.json.message.contains('Please select at least one user.')
    }

    @DirtiesRuntime
    void "test modifyRole() if RoleIds are removed during authentication"() {
        given: 'Admin role id for modification'
        mockSpringSecurityUtils(true)
        controller.request.json = [ userIds: [trialUser.id, normalUser.id],
                roleIds: [adminRole.id, userManagerRole.id, userRole.id]
        ]
        controller.request.method = 'POST'

        int index = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (index == 1) {
                index++
                return [trialUser.id]
            }
            return []
        } ] as UserManagementService

        when: 'Manager tries to set Admin Role to 2 Normal users'
        controller.modifyRoles()

        then: 'Admin role should not be applied to the 2 Normal Users'
        controller.response.json.success == false
        controller.response.json.message.contains('No Roles selected.')
    }

    @DirtiesRuntime
    void "test modifyRole() if ADMIN user tries to assign Admin RoleIds in Refresh mode"() {
        given: 'Admin roleId for role modification'
        controller.request.json = [roleIds: [adminRole.id], userIds: [normalUser.id, trialUser.id],
                roleActionType: 'refresh']
        mockSpringSecurityUtils(true)
        int index = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (index == 1) {
                index++
                return [normalUser.id, trialUser.id]
            }
            return [adminRole.id]
        } ] as UserManagementService

        controller.request.method = 'POST'

        when: 'Admin tries to set Admin Role to 2 Normal users'
        controller.modifyRoles()

        then: 'Only Admin role should be applied to the 2 Normal Users'
        controller.response.json.success == true

        Set<Role> trialUserAuthorities = trialUser.authorities
        adminRole in trialUserAuthorities
        userRole in trialUserAuthorities == false

        Set<Role> normalUserAuthorities = normalUser.authorities
        adminRole in normalUserAuthorities
        userRole in normalUserAuthorities == false
    }

    @DirtiesRuntime
    void "test modifyRole() if ADMIN user tries to assign Admin RoleIds in Append mode"() {
        given: 'Admin role id for modification'
        controller.request.json = [roleIds: [adminRole.id], userIds: [normalUser.id, trialUser.id],
                roleActionType: 'refresh']
        mockSpringSecurityUtils(true)
        int index = 1
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            if (index == 1) {
                index++
                return [normalUser.id, trialUser.id]
            }
            return [adminRole.id, userRole.id]
        } ] as UserManagementService

        controller.request.method = 'POST'

        when: 'Admin tries to set Admin Role to 2 Normal users'
        controller.modifyRoles()

        then: 'Admin role should not be applied to the 2 Normal Users'
        controller.response.json.success == true

        // Already assigned roles also exists'
        Set<Role> trialAuthorities = trialUser.authorities
        adminRole in trialAuthorities
        userRole in trialAuthorities

        Set<Role> userAuthorities = normalUser.authorities
        adminRole in userAuthorities
        userRole in userAuthorities
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() when exception is thrown"() {
        // USER_MANAGER with Admin role is currently logged in
        given:'Mocking SpringSecurityUtils'
        mockSpringSecurityUtils(true)
        assignRolesToUsers(adminRole, userRole, userRole)

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [adminUser.id, trialUser.id, normalUser.id]
        } ] as UserManagementService

        User.metaClass.'static'.withCriteria = { Closure closure ->
            throw new SQLException()
        }

        controller.request.method = 'POST'

        and: 'Request userIds for activation and deactivation'
        controller.request.json = [selectedIds: [adminUser.id, trialUser.id, normalUser.id], lockAccount: true]

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Error message is displayed'
        controller.response.status == HttpStatus.NOT_ACCEPTABLE.value()
        controller.response.json.success == false
        controller.response.json.message.contains('Unable to enable/disable the user. Please try again.')

        !adminUser.refresh().accountLocked
        !trialUser.refresh().accountLocked
        !normalUser.refresh().accountLocked
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() to make users active"() {
        // USER_MANAGER with Admin role is currently logged in
        given: 'Mocking SpringSecurityUtils'
        mockSpringSecurityUtils(true)
        adminUser.setAccountLocked(false)
        normalUser.setAccountLocked(false)
        trialUser.setAccountLocked(false)
        assignRolesToUsers(adminRole, userRole, userRole)

        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return [adminUser.id, trialUser.id, normalUser.id]
        } ] as UserManagementService

        controller.request.method = 'POST'

        and: 'Request userIds for activation and deactivation'
        controller.request.json = [selectedIds: [adminUser.id, trialUser.id, normalUser.id], lockAccount: true]

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Success message is displayed'
        controller.response.status == 200
        controller.response.json.success == true

        adminUser.refresh().accountLocked
        trialUser.refresh().accountLocked
        normalUser.refresh().accountLocked
    }

    @DirtiesRuntime
    void "test lockUnlockUserAccounts() when mongodb is used as database"() {
        given: 'mocked instance'
        // USER_MANAGER with Admin role is currently logged in
        mockSpringSecurityUtils(true)
        controller.userManagementService = [getAppropiateIdList: { List ids ->
            return ['507f191e810c19729de860ea', '4cdfb11e1f3c000000007822', '4abcd11e1f3c000000007823']
        } ] as UserManagementService

        User.metaClass.'static'.getCollection = {
            return ['update': { def query, def update, final boolean upsert, final boolean multi ->
                [adminUser, normalUser, trialUser].each { User userInstance ->
                    userInstance.accountLocked = false
                    userInstance.save()
                }
                return [n: 3]
            } ]
        }
        grailsApplication.config.cc.plugins.crm.persistence.provider = 'mongodb'
        controller.request.method = 'POST'

        and: 'Request userIds for activation and deactivation'
        controller.request.json = [selectedIds: ['507f191e810c19729de860ea', '4cdfb11e1f3c000000007822',
                '4abcd11e1f3c000000007823'], lockAccount: true]

        when: 'Action is hit'
        controller.lockUnlockUserAccounts()

        then: 'Users are disabled'
        adminUser.refresh().accountLocked == false
        trialUser.refresh().accountLocked == false
        normalUser.refresh().accountLocked == false
        controller.response.json.success == true
    }

    void "test updateEmail action for invalid or missing data"() {
        given: 'request to updateEmail with missing data'
        controller.request.json = ['id': null, 'newEmail': null, 'confirmNewEmail': null]
        controller.request.method = 'POST'

        when: 'Action is hit'
        controller.updateEmail()

        then: 'Error message must be displayed'
        controller.response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
        controller.response.json.message.contains('Please select a user and enter new & confirmation email.')
    }

    void "test updateEmail action when email does not match the confirm email"() {
        given: 'request with different email and confirm email'
        controller.request.json = ['id': adminUser.id, 'newEmail': 'admin@causecode.com',
                'confirmNewEmail': 'adm@causecode.com']

        when: 'Action is hit'
        controller.request.method = 'POST'
        controller.updateEmail()

        then: 'Error message must be displayed'
        controller.response.status == HttpStatus.PRECONDITION_FAILED.value()
        controller.response.json.message.contains('Email does not match the Confirm Email.')
    }

    @DirtiesRuntime
    void "test updateEmail when user with entered email already exist"() {
        given: 'request with existing email id'
        controller.request.json = ['id': adminUser.id, 'newEmail': 'admin@causecode.com',
                'confirmNewEmail': 'admin@causecode.com']
        controller.request.method = 'POST'

        User.metaClass.'static'.countByEmail = { String newEmail ->
            return true
        }

        when: 'Action is hit'
        controller.updateEmail()

        then: 'Error message must be displayed'
        controller.response.status == HttpStatus.CONFLICT.value()
        controller.response.json.message.contains('User already exists with Email')
    }

    @DirtiesRuntime
    void "test updateEmail when user with entered email is not found"() {
        given: 'request with existing email id'
        controller.request.json = ['id': adminUser.id, 'newEmail': 'admin@causecode.com',
                'confirmNewEmail': 'admin@causecode.com']
        controller.request.method = 'POST'

        User.metaClass.'static'.get = { Integer id ->
            return null
        }

        when: 'Action is hit'
        controller.updateEmail()

        then: 'Error message must be displayed'
        controller.response.status == HttpStatus.NOT_FOUND.value()
        controller.response.json.message.contains('User not found with id')
    }

    void "test updateEmail when userInstance is valid"() {
        given: 'request with different email and confirm email'
        controller.request.json = ['id': adminUser.id, 'newEmail': 'admin@causecode.com',
                'confirmNewEmail': 'admin@causecode.com']
        controller.request.method = 'POST'

        when: 'Action is hit'
        controller.updateEmail()

        then: 'Success message is displayed'
        controller.response.status == 200
        controller.response.json.message.contains('Email updated Successfully.')
    }

    void "test updateEmail action when userInstance could not be saved properly"() {
        given: 'Mocked get method'
        User.metaClass.'static'.get = { Integer id ->
            adminUser.username = null
            return adminUser
        }
        when: 'updateEmail action is hit'
        controller.request.json = ['id': adminUser.id, 'newEmail': 'admin@causecode.com',
                'confirmNewEmail': 'admin@causecode.com', username: null]
        controller.request.method = 'POST'
        controller.updateEmail()

        then: 'Error message must be displayed'
        controller.response.status == HttpStatus.NOT_ACCEPTABLE.value()
        controller.response.json.message.contains('Unable to update user\'s email.')
    }
}

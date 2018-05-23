package com.causecode.user

import grails.buildtestdata.BuildDataTest
import grails.buildtestdata.mixin.Build
import grails.plugin.springsecurity.SpringSecurityService
import grails.test.runtime.DirtiesRuntime
import grails.testing.services.ServiceUnitTest
import groovy.json.JsonBuilder
import spock.lang.Specification

/**
 * This class specifies unit test cases for {@link com.causecode.user.UserManagementService}.
 */
@Build([User, UserRole, Role])
class UserManagementServiceSpec extends Specification implements ServiceUnitTest<UserManagementService>, BuildDataTest {

    User adminUser
    User normalUser
    User managerUser
    Role userRole
    Role adminRole
    SpringSecurityService springSecurityServiceForAdminUser, springSecurityServiceForNormalUser,
            springSecurityServiceForManagerUser

    def setup() {
        adminUser = new User([username: 'admin', password: 'admin@13', email: 'bootstrap@causecode.com',
                firstName: 'adminCausecode', lastName: 'adminCausecode', gender: 'male', enabled: true])
        springSecurityServiceForAdminUser = new SpringSecurityService()
        springSecurityServiceForAdminUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        adminUser.springSecurityService = springSecurityServiceForAdminUser
        assert adminUser.save(flush: true)

        normalUser = new User([username: 'normalUser', password: 'normalUser@132',
                email: 'normalUserbootstrap@causecode.com', firstName: 'normalUserCauseCode',
                lastName: 'normalUserTechnologies', gender: 'male', enabled: true])
        springSecurityServiceForNormalUser = new SpringSecurityService()
        springSecurityServiceForNormalUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        normalUser.springSecurityService = springSecurityServiceForNormalUser
        assert normalUser.save(flush: true)

        managerUser = new User([username: 'managerUser', password: 'managerUser@134',
                 email: 'managerUserbootstrap@causecode.com', firstName: 'managerUserCauseCode',
                 lastName: 'managerUserTechnologies', gender: 'male', enabled: true])
        springSecurityServiceForManagerUser = new SpringSecurityService()
        springSecurityServiceForManagerUser.metaClass.encodePassword = { String password -> 'ENCODED_PASSWORD' }
        managerUser.springSecurityService = springSecurityServiceForManagerUser
        assert managerUser.save(flush: true)

        adminRole = Role.findOrSaveByAuthority('ROLE_ADMIN')
        userRole = Role.findOrSaveByAuthority('ROLE_USER')
    }

    void mockExecuteQueryTotalCount(int totalCount) {
        UserRole.metaClass.static.executeQuery = { String query, Map stringQueryParams ->
            return [totalCount]
        }
    }

    void mockExecuteQueryGetUsers(List usersList) {
        UserRole.metaClass.static.executeQuery = { String query, Map stringQueryParams, Map params1 ->
            return usersList
        }
    }

    @DirtiesRuntime
    void "test listForMysql with params roleType anyGranted"() {
        given: 'parameter map'
        Map params = [ offset: 0, max: 15, roleFilter: [userRole.id.toString()], roleType: 'Any Granted']
        mockExecuteQueryGetUsers([adminUser, normalUser])
        mockExecuteQueryTotalCount(2)

        when: 'listForMySql method called'
        Map result = service.listForMysql(params)

        then: 'List of users will be returned'
        !result.isEmpty()
        result.instanceList[0] == adminUser
        result.instanceList[1] == normalUser
        result.totalCount == 2
    }

    @DirtiesRuntime
    void "test listForMysql with params roleType allGranted"() {
        given: 'parameter map'
        Map params = [ offset: 0, max: 15, roleFilter: [userRole.id.toString()], roleType: 'All Granted']
        mockExecuteQueryGetUsers([adminUser, normalUser])
        mockExecuteQueryTotalCount(2)

        when: 'listForMySql method called'
        Map result = service.listForMysql(params)

        then: 'List of users will be returned'
        !result.isEmpty()
        result.instanceList[0] == adminUser
        result.instanceList[1] == normalUser
        result.totalCount == 2
    }

    @DirtiesRuntime
    void "test getList with mongo as parameter"() {
        given: 'Instance of User and Admin with appropriate roles'
        assert UserRole.create(adminUser, adminRole, true)
        assert UserRole.create(normalUser, userRole, true)

        UserRole.metaClass.static.createCriteria = {
            return ['list': { Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                return [adminUser]
            } ]
        }
        User.metaClass.static.createCriteria = {
            return ['list': { Map map, Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                List list = [adminUser]
                list.metaClass.getTotalCount = {
                    return 1
                }
                return list
            } ]
        }

        when: 'getList method is called and roleFilter is passed as List'
        Map params = [ offset: 0, max: 15, dbType: 'Mongo',
                roleFilter: [adminRole.id.toString()], roleType: 'Any Granted']
        Map result = service.getList(params)

        then: 'Result map is returned'
        result['totalCount'] == 1
        result['instanceList'].get(0) == adminUser

        when: 'getList method is called and roleFilter is passed as String'
        params = [offset: 0, max: 15, dbType: 'Mongo', roleFilter: adminRole.id.toString(), roleType: 'Any Granted']
        result = service.getList(params)

        then: 'Result map is returned'
        result['totalCount'] == 1
        result['instanceList'].get(0) == adminUser
    }

    @DirtiesRuntime
    void "test getList with mongo when roleType is not passed at all"() {
        given: 'Instance of User and Admin with appropriate roles'
        Map params = [ offset: 0, max: 15, dbType: 'Mongo', roleFilter: adminRole.id.toString()]
        assert UserRole.create(adminUser, adminRole, true)
        assert UserRole.create(normalUser, userRole, true)

        UserRole.metaClass.static.createCriteria = {
            return ['list': { Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                return [adminUser, normalUser]
            } ]
        }
        User.metaClass.static.createCriteria = {
            return ['list': { Map map, Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                List list = [adminUser, normalUser]
                list.metaClass.getTotalCount = {
                    return 2
                }
                return list
            } ]
        }

        when: 'getList method is called'
        Map result = service.getList(params)

        then: 'Result map is returned'
        result['totalCount'] == 2
        result['instanceList'].size() == 2
    }

    @DirtiesRuntime
    void "test getList when roleFilter is not passed at all"() {
        given: 'Instance of User and Admin with appropriate roles'
        Map params = [offset: 0, max: 15, letter: 'a', roleFilter: adminRole.id.toString(),
                query: 'adm', dbType: 'Mongo']
        assert UserRole.create(adminUser, adminRole, true)
        assert UserRole.create(normalUser, adminRole, true)

        UserRole.metaClass.static.createCriteria = {
            return ['list': { Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                return [adminUser, normalUser]
            } ]
        }
        User.metaClass.static.createCriteria = {
            return ['list': { Map map, Closure closure ->
                assert closure != null
                new JsonBuilder() closure
                List list = [adminUser]
                list.metaClass.getTotalCount = {
                    return 1
                }
                return list
            } ]
        }

        when: 'getList method is called'
        Map result = service.getList(params)

        then: 'Result map is returned'
        result['totalCount'] == 1
        result['instanceList'].get(0) == adminUser
    }

    void "test getAppropriatedList with params"() {
        given: 'List of user ids'
        List ids = [adminUser.id, normalUser.id, managerUser.id] // ["adminUser"] Fails

        when: 'getAppropriatedList method called'
        List result = service.getAppropiateIdList(ids)

        then: 'List of users will be returned'
        assert !result.isEmpty()
    }

    void "test getAppropriatedList with null list ids"() {
        when: 'getAppropriatedList method called'
        List result = service.getAppropiateIdList(null)

        then: 'Empty list will be returned'
        assert result.isEmpty()
    }

    void "test getAppropriatedList with ids for mongo"() {
        given: 'List of user ids for Mongo'
        List ids = ['507f191e810c19729de860ea', '4cdfb11e1f3c000000007822', '4cdfb11e1f3c000000007822']

        when: 'getAppropriatedList method is called'
        List result = service.getAppropiateIdList(ids)

        then: 'List with unique ids is returned'
        assert !result.isEmpty()
        assert result.size() == 2
    }

    void "test getList with params"() {
        given: 'User instance list and params'
        Map params = [ offset: 0, max: 15, roleFilter: [userRole.id.toString()], roleType: 'Any Granted']
        List userInstanceList = [adminUser, normalUser, managerUser]
        service.metaClass.listForMysql = { Map params1 ->
            return [instanceList: userInstanceList, totalCount: userInstanceList.size()]
        }

        when: 'getList method called'
        Map result = service.getList(params)

        then: 'List of users will be returned'
        assert !result.isEmpty()
    }

    void "test getList with letter and query in params"() {
        given: 'Params map'
        Map params = [ offset: 0, max: 15, letter: 'a']
        assert UserRole.create(normalUser, userRole, true)
        assert UserRole.create(managerUser, userRole, true)
        assert UserRole.create(adminUser, adminRole, true)

        mockExecuteQueryGetUsers([adminUser])
        mockExecuteQueryTotalCount(1)

        when: 'getList method called with letter in params'
        Map result = service.getList(params)

        then: 'List of users will be returned'
        assert !result.isEmpty()
        result['totalCount'] == 1
        result['instanceList'].get(0) == adminUser

        when: 'getList method called with query in params '
        params['letter'] = ''
        params['query'] = 'adm'
        result = null
        result = service.getList(params)

        then: 'List of users matching the criteria will be returned'
        assert !result.isEmpty()
        result['totalCount'] == 1
        result['instanceList'].get(0) == adminUser
    }

    void "test getSelectedItemList with arguments"() {
        given: 'SelectedIds and parameter map'
        String selectedIds = adminUser.id + ',' + normalUser.id + ',' + managerUser.id
        Map args = [offset: 0, max: 15, roleFilter: [userRole.id.toString()], roleType: '']

        mockExecuteQueryGetUsers([adminUser, normalUser, managerUser])
        mockExecuteQueryTotalCount(3)

        when: 'getAppropriatedList method called'
        List result = service.getSelectedItemList(true, selectedIds, args)

        then: 'List of users will be returned'
        assert !result.isEmpty()

        when: 'getSelectedItemList method is called'
        result = service.getSelectedItemList(false, selectedIds, args)

        then: 'All users with provided ids will be returned as result'
        assert !result.isEmpty()
        result.get(0).id == adminUser.id
        result.get(1).id == normalUser.id
        result.get(2).id == managerUser.id

        when: 'getSelectedItemList method is called with selectAll= false and selectedIds as empty'
        result = service.getSelectedItemList(false, '', args)

        then: 'Empty list is returned'
        assert result.isEmpty()
    }
}

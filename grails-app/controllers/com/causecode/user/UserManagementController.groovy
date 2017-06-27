/*
 * Copyright (c) 2016, CauseCode Technologies Pvt Ltd, India.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are not permitted.
 */
package com.causecode.user

import com.causecode.RestfulController
import com.causecode.exceptions.DBTypeNotFoundException
import com.causecode.util.NucleusUtils
import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.annotation.Secured
import grails.plugins.export.ExportService
import org.springframework.http.HttpStatus

/**
 * @author Vishesh Duggar
 * @author Shashank Agrawal
 * @author Laxmi Salunkhe
 */
@Secured(['ROLE_USER_MANAGER'])
class UserManagementController extends RestfulController {

    static namespace = 'v1'
    UserManagementController() {
        super(User)
    }

    ExportService exportService
    UserManagementService userManagementService

    static responseFormats = ['json']

    /*
     * Method that excludes admin user's ids from given list of user ids
     *
     * @params List userIds
     *
     * @return List userIds (excluded admin ids)
     */
    private List removeAdminIds(List userIds) {
        Role adminRole = Role.findByAuthority('ROLE_ADMIN')
        List adminUsersIds = UserRole.findAllByRole(adminRole)*.user*.id

        log.info "Removing admin user ids: $adminUsersIds."

        List nonAdminUserIds = userIds - adminUsersIds
        log.info "Removed admin users: $nonAdminUserIds"

        return nonAdminUserIds
    }

    /**
     * List action used to fetch Role list and User's list with filters and pagination applied.
     * @param max Integer parameter used to set number of records to be returned.
     * @param dbType Type of database support. Must be either "Mongo" or "Mysql".
     * @return Result in JSON format.
     */
    def index(Integer max, int offset, String dbType) {
        String tempDbType
        try{
            tempDbType=NucleusUtils.getDBType()
            log.info "database name received from NucleusUtils : $tempDbType"
        }
        catch (DBTypeNotFoundException ex){
            respondData([message: ex.message], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }
        params.offset = offset ?: 0
        params.max = Math.min(max ?: 10, 100)
        params.sort = params.sort ?: 'dateCreated'
        params.order = params.order ?: 'desc'
        tempDbType = tempDbType ?: 'Mysql'

        log.info "Params received to fetch users :$params"

        Map result = userManagementService."listFor${tempDbType}"(params)
        if (offset == 0) {
            result['roleList'] = Role.list(max: 50)
        }

        render result as JSON
    }

    /**
     * Modifies Roles of users with help of given roles and type.
     * @param userIds List of users ID
     * @param roleIds List of role ID
     * @param roleActionType String value which specifies two conditions.
     * 1. "refresh" - Remove existing roles and apply new roles.
     * 2. other role type - Update existing roles. i.e. append roles.
     * @return Renders boolean response True.
     */
    def modifyRoles() {
        params.putAll(request.JSON)
        log.info "Parameters recevied to modify roles: $params"

        Set failedUsersForRoleModification = []
        List userIds = userManagementService.getAppropiateIdList(params.userIds)
        List roleIds = userManagementService.getAppropiateIdList(params.roleIds)
        roleIds = roleIds*.toLong()

        if (!SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
            userIds = removeAdminIds(userIds)
            /*
             * If a User is trying to assign ADMIN Role to any User, he should not be allowed to do so.
               Only ADMIN Users can assign ADMIN role to other Users.
            */
            Role adminRole = Role.findByAuthority('ROLE_ADMIN')
            roleIds -= adminRole.id
            log.info '[Not authorized] Removing Admin Role ids'
        }

        if (!userIds) {
            String message = 'Please select at least one user.'

            if (!SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
                message += 'Users with role Admin are excluded from selected list.'
            }

            respondData([success: false, message: message], [status: HttpStatus.NOT_ACCEPTABLE])
            return
        }

        if (!roleIds) {
            String message = 'No Roles selected.'

            if (!SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
                message += 'Only Users with Admin role can assign Admin roles.'
            }

            respondData([success: false, message: message], [status: HttpStatus.NOT_ACCEPTABLE])
            return
        }

        List roleInstanceList = Role.getAll(roleIds)

        userIds.each { userId ->
            User userInstance = User.get(userId)
            if (params.roleActionType == 'refresh') {
                UserRole.removeAll(userInstance)
            }
            roleInstanceList.each { roleInstance ->
                UserRole userRoleInstance = UserRole.findByUserAndRole(userInstance, roleInstance)
                // To avoid MySQLIntegrityConstraintViolationException which occurs if duplicate record is inserted
                userRoleInstance = userRoleInstance ?: UserRole.create(userInstance, roleInstance, true)
                if (!userRoleInstance || userRoleInstance.hasErrors()) {
                    failedUsersForRoleModification << userInstance.email
                }
            }
        }

        if (failedUsersForRoleModification) {
            respondData([success: false, message: 'Unable to grant role for users with email(s)' +
                    "${failedUsersForRoleModification.join(', ')}."], [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return
        }

        respondData([success: true, message: 'Roles updated succesfully.'])
    }

    /**
     * Lock/Unlock user accounts.
     * @param selectedIds List of user IDs
     * @param lockAccount boolean value which specifies two conditions.
     * 1. true - Set User field lockAccount to true.
     * 2. false - Set User field lockAccount to false.
     * @return Renders message response in JSON format.
     */
    // In case anything goes wrong while running database queries.
    @SuppressWarnings('CatchException')
    def lockUnlockUserAccounts() {
        params.putAll(request.JSON)
        boolean lockAccount = params.lockAccount

        log.info "Params received to lock/unlock user account: $params"

        // TODO Change cc package name to causecode after changes in crm plugin
        boolean useMongo = grailsApplication.config.cc.plugins.crm.persistence.provider == 'mongodb'

        List selectedUserIds = userManagementService.getAppropiateIdList(params.selectedIds)

        if (!SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
           selectedUserIds = removeAdminIds(selectedUserIds)
        }

        if (!selectedUserIds) {
            String message = 'Please select at least one user.'
            if (!SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')) {
                message += 'Users with role Admin are excluded from selected list.'
            }
            respondData([success: false, message: message], [status: HttpStatus.NOT_ACCEPTABLE])

            return false
        }

        try {
            if (useMongo) {
                // Returns http://api.mongodb.org/java/current/com/mongodb/WriteResult.html
                def writeResult = User.collection.update([_id: [$in: selectedUserIds]],
                        [$set: [accountLocked: lockAccount]], false, true)

                int updatedFields =  writeResult.n

                respondData([message: "Total $updatedFields user's account set to $lockAccount successfully.",
                        success: true])
            } else {
                selectedUserIds = selectedUserIds*.toLong()

                List<User> userList = User.withCriteria {
                    'in'('id', selectedUserIds)

                    maxResults(1000)
                } as List
                userList.each { User userInstance ->
                    userInstance.accountLocked = lockAccount
                    userInstance.save()
                }

                respondData([message: "Successfully updated user's account status.", success: true])
            }
        } catch (Exception e) {
            log.error 'Error lock/unlock user.', e

            respondData([message: 'Unable to enable/disable the user. Please try again.', success: false],
                    [status: HttpStatus.NOT_ACCEPTABLE])
        }
    }

    /**
     * This action provides excel report for given listed users.
     * @param selectedUser List of users ID
     */
    def export(boolean selectAll) {
        Map parameters
        Map labels = [:]
        List fields = [], columnWidthList = []
        List<User> userList = userManagementService.getSelectedItemList(selectAll, params.selectedIds, params)
        float pointOne = 0.1f, pointTwo = 0.2f, pointThree = 0.3f
        String id = 'id', email = 'email', firstName = 'firstName', lastName = 'lastName',
                gender = 'gender', birthdate = 'birthdate', dateCreated = 'dateCreated',
                enabled = 'enabled', accountLocked = 'accountLocked'
        fields << id; labels.id = 'User Id'; columnWidthList << pointOne
        fields << email; labels.email = 'Email'; columnWidthList << pointThree
        fields << firstName; labels.firstName = 'First Name'; columnWidthList << pointTwo
        fields << lastName; labels.lastName = 'Last Name'; columnWidthList << pointTwo
        fields << gender; labels.gender = 'Gender'; columnWidthList << pointOne
        fields << birthdate; labels.birthdate = 'Birthdate'; columnWidthList << pointTwo
        fields << dateCreated; labels.dateCreated = 'Date Joined'; columnWidthList << pointTwo
        fields << enabled; labels.enabled = 'Active'; columnWidthList << pointOne
        fields << accountLocked; labels.accountLocked = 'Locked'; columnWidthList << pointOne

        parameters = ['column.widths': columnWidthList]

        response.contentType = 'application/vnd.ms-excel'
        response.setHeader('Content-disposition', 'attachment; filename=user-report.csv')

        exportService.export('csv', response.outputStream, userList, fields, labels, [:], parameters)
    }

    /**
     * Used to update the email of any given user.
     * @param id Identity of user to update.
     * @param newEmail New email address to update
     * @param confirmNewEmail Confirm new email
     * @return Status message with success or not.
     */
    def updateEmail() {
        params.putAll(request.JSON as Map)
        log.debug "Params received to update email: $params"

        if (!params.id || !params.newEmail || !params.confirmNewEmail) {
            respondData([message: 'Please select a user and enter new & confirmation email.'],
                    [status: HttpStatus.UNPROCESSABLE_ENTITY])

            return false
        }

        params.newEmail = params.newEmail.toLowerCase()
        params.confirmNewEmail = params.confirmNewEmail.toLowerCase()

        if (params.newEmail != params.confirmNewEmail) {
            respondData([message: 'Email does not match the Confirm Email.'], [status: HttpStatus.PRECONDITION_FAILED])

            return false
        }

        if (User.countByEmail(params.newEmail)) {
            respondData([message: "User already exists with Email: $params.newEmail"],
                    [status: HttpStatus.CONFLICT])

            return false
        }

        User userInstance = User.get(params.id)
        if (!userInstance) {
            respondData([message: "User not found with id: $params.id."], [status: HttpStatus.NOT_FOUND])

            return false
        }

        userInstance.email = params.newEmail
        if (!NucleusUtils.save(userInstance, true)) {
            log.warn "Error saving $userInstance $userInstance.errors"
            respondData([message: "Unable to update user's email.", error: userInstance.errors],
                    [status: HttpStatus.NOT_ACCEPTABLE])

            return false
        }

        respondData([message: 'Email updated Successfully.'])
    }
}

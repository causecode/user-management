# ChangeLog

## [2.0.0] - [Unreleased]
### Changed
- Upgraded the plugin to support grails 3.3.5

## [1.0.2] - [02-04-2018]

### Added
- CircleCI and code climate configurations.

### Removed
- Unnecessary dependencies.

## [1.0.1] - [13-03-2018]

### Added
1. Handled `isAccountExpired` and `isAccountLocked` fields in the Marshaller.

## [1.0.0] - 24-01-2018

### Fixed
1. Handled issue related to ObjectIDs while responding the user instance. 

## [0.0.9] - 12-10-2017

### Fixed
1. Dependencies - changed compile dependencies to provided dependency for common dependencies which will be
present in the installing app. (For ex - spring security core and spring security rest, json views etc.)

### Removed
1. `org.grails.plugins:export` dependency which was added by nucleus plugin has been removed.
(It should have been present in user-management plugin itself.)
2. The export endpoint to export user data in csv format present in UserManagementController has been removed.
The export plugin was too bulky. It was adding 10 mb to the app's war size.
3. Google re-captcha server side validation from signup endpoint in UserController. It will be added back after
a separate gradle plugin is created to support server side google re-captcha validation.

### Added
* ####CircleCI configuration
    -  `.circleci/config.yml` for build automation using `CircleCI`.
    - `mavenCredsSetup.sh` for generating `gradle.properties` during the CircleCI build.

### Modified
* Upgraded `gradle-code-quality` to `v1.0.0`.

## Version 0.0.8

### Modification

1. Updated endpoint save() in UserController which took dbType as param and replaced with NucleusUtils.getDBType method.
2. Updated endpoint index() in UserManagementController which took dbType as param and replaced with NucleusUtils.getDBType method.
3. Updated the test cases in class UserControllerSpec which were passing dbType as params and replaced it with mock config properties.
4. Updated the test cases in class UserManagementControllerSpec which were passing dbType as params and replaced it with mock config properties.
5. Added a test case in the class UserManagementControllerSpec to check for the valid exception thrown of type DBTypeNotFoundException when database cannot be inferred from config properties.

NOTE
User-management 0.0.8 version is only compatible with Nucleus version - 0.4.10

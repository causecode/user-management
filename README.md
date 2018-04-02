# User Management Plugin

[![Maintainability](https://api.codeclimate.com/v1/badges/a543c502c3af485cd20c/maintainability)](https://codeclimate.com/repos/5abe31323a371e029300214d/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/a543c502c3af485cd20c/test_coverage)](https://codeclimate.com/repos/5abe31323a371e029300214d/test_coverage)

```
Version - 1.0.2
Grails Version - 3.2.0 > *
Author - CauseCode Technologies
```

### Installation 
 
Add this to the dependencies block of your `build.gradle` file.

```
dependencies {
    compile "com.causecode.plugins:user-management:$VERSION" 
}
```

### About
This Plugin provides endpoints for User related operations such as `SignUp, ForgotPassword, ResetPassword,` etc.
This plugin also provides functionality to manage user roles.
This Plugin is designed for `Stateless REST APIs` only and hence does not make use of `HTTP Sessions`.

### Usage
The SignUp endpoint provides 3 hooks namely preUserSignup, onCreateUser and postUserSignup.
See DefaultUserHookService.groovy for default implementation of these hooks.

### Implementing in your Grails Application

### Overriding UserHookService

`UserHookService.groovy` 

This interface declares the methods required for UserHookService. This plugin defines a
 `DefaultUserHookService` class which implements this interface and is registered as the `userHookService` bean.
 To override the default implementation of `userHookService` bean, the installing application should create a
 class(say CustomUserHookService) which implements this interface and finally override the injected bean for
 custom class (CustomUserHookService) as `userHookService` in `resources.groovy`.
 
 ```
beans = {
    userHookService(CustomUserHookService)
}
```
    
### Disable SignUp

To disable SignUp throw SignUpNotAllowed Exception from your overridden userHookService's **preSignup** method.
This Exception is handled in UserController such that it responds with Http status code 423(LOCKED) with exception 
message.

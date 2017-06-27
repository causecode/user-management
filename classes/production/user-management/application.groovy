grails.serverURL = 'http://localhost:8080'

dataSource {
    pooled = true
    driverClassName = 'org.h2.Driver'
    username = 'sa'
    password = ''
    dbCreate = 'create-drop'
    url = 'jdbc:h2:mem:devDb'
}

hibernate {
    cache {
        use_second_level_cache = false
        use_query_cache = false
    }
}
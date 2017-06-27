databaseChangeLog = {

    changeSet(author: "Ankit Agrawal", id: "20012017-01") {
        createTable(tableName: 'authentication_token') {
            column(autoIncrement: 'true', name: 'id', type: 'BIGINT') {
                constraints(primaryKey: 'true', primaryKeyName: "authentication_tokenPK")
            }

            column(name: 'date_created', type: 'DATETIME') {
                constraints(nullable: 'false', index: true)
            }


            column(name: 'last_updated', type: 'DATETIME') {
                constraints(nullable: 'false', index: true)
            }

            column(name: 'email', type: 'VARCHAR(255)') {
                constraints(nullable: 'false', index: true)
            }

            column(name: 'access_count', type: 'INT') {
                constraints(nullable: 'false')
            }

            column(name: 'token', type: 'VARCHAR(255)') {
                constraints(nullable: 'false', unique: 'true', index: true)
            }
        }
    }
}
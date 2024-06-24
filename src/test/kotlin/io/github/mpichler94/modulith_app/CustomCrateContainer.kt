package io.github.mpichler94.modulith_app

import org.testcontainers.cratedb.CrateDBContainer
import org.testcontainers.utility.DockerImageName

class CustomCrateContainer : CrateDBContainer {
    constructor(dockerImageName: String?) : super(dockerImageName)

    constructor(dockerImageName: DockerImageName?) : super(dockerImageName)

    override fun getDriverClassName(): String {
        return "io.crate.client.jdbc.CrateDriver"
    }

    override fun getJdbcUrl(): String {
        val additionalUrlParams = this.constructUrlParameters("?", "&")
        return "jdbc:crate://" + this.host + ":" + this.getMappedPort(5432) + "/" + databaseName + additionalUrlParams
    }
}
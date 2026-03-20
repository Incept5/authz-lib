package org.incept5.authz

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test

@QuarkusTest
class SecureServiceTest {


    @Test
    fun `should return 200 when calling secured method - backoffice admin`() {
        //given:
        val id = "123"

        //when: call secured method
        val response = RestAssured.given()
            .`when`()
            .header("Authorization","Bearer backoffice-admin-token")
            .get("/example/secure/$id")

        //then: verify response
        response.then()
            .statusCode(200)
            .body(CoreMatchers.equalTo("Authorized method called with id: $id"))
    }

    @Test
    fun `should return 403 when calling secured method - user has no roles`() {
        //given:
        val id = "123"

        //when: call secured method
        val response = RestAssured.given()
            .`when`()
            .header("Authorization","Bearer no-roles-token")
            .get("/example/secure/$id")

        //then: verify response
        response.then()
            .statusCode(403)
    }

    @Test
    fun `should return 401 when calling secured method - no auth header`() {
        //given:
        val id = "123"

        //when: call secured method
        val response = RestAssured.given()
            .`when`()
            .get("/example/secure/$id")

        //then: verify response
        response.then()
            .statusCode(401)
    }

    @Test
    fun `should return 401 when calling secured method - invalid token`() {
        //given:
        val id = "123"

        //when: call secured method
        val response = RestAssured.given()
            .`when`()
            .header("Authorization","Bearer invalid-token")
            .get("/example/secure/$id")

        //then: verify response
        response.then()
            .statusCode(401)
    }

    @Test
    fun `should return principal info from SecurityContext`() {
        val response = RestAssured.given()
            .`when`()
            .header("Authorization", "Bearer backoffice-admin-token")
            .get("/example/me")

        response.then()
            .statusCode(200)
            .body("name", CoreMatchers.equalTo("backoffice user"))
            .body("globalRoles[0]", CoreMatchers.equalTo("backoffice.admin"))
            .body("isAdmin", CoreMatchers.equalTo(true))
    }

    @Test
    fun `should return 401 for me endpoint without token`() {
        val response = RestAssured.given()
            .`when`()
            .get("/example/me")

        response.then()
            .statusCode(401)
    }
}
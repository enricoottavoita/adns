package com.eyalm.adns.data.nextdns.api

import com.eyalm.adns.domain.nextdns.ApiResult
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class NextDnsResponseMapperTest {
    @Test
    fun `successful http response with errors body remains a failure`() {
        val body = JsonParser.parseString(
            """{"errors":[{"code":"duplicate"}]}"""
        ).asJsonObject

        val result = Response.success(body).toJsonApiResult()

        assertTrue(result is ApiResult.ServerFailure)
        result as ApiResult.ServerFailure
        assertEquals(200, result.status)
        assertEquals("duplicate", result.problems.single().code)
    }

    @Test
    fun `successful data body is preserved`() {
        val body = JsonParser.parseString("""{"data":[]}""").asJsonObject

        val result = Response.success(body).toJsonApiResult()

        assertTrue(result is ApiResult.Success)
        assertEquals(body, (result as ApiResult.Success).value)
    }
}

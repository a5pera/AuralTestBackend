package com.tencent.wxcloudrun.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ApiResponseTest {

    @Test
    void successResponseUsesZeroCode() {
        ApiResponse response = ApiResponse.ok("ok");

        assertEquals(0, response.getCode());
    }

    @Test
    void errorResponseDoesNotUseSuccessCode() {
        ApiResponse response = ApiResponse.error("DENY");

        assertNotEquals(0, response.getCode());
        assertEquals("DENY", response.getErrorMsg());
    }
}

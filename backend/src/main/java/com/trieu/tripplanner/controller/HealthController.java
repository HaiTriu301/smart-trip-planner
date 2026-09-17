package com.trieu.tripplanner.controller;

import com.trieu.tripplanner.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary endpoint to check the response envelope end to end (Task 0.4, called by the frontend in Task 0.5).
 */
@Tag(name = "Health", description = "Connectivity check")
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @Operation(summary = "Ping the API", description = "Returns \"pong\" inside the standard ApiResponse envelope.")
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }

}

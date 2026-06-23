package com.example.fraudengine.web.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} processing in {@code @WebMvcTest} slices.
 *
 * <p>The production {@link com.example.fraudengine.config.SecurityConfig} carries
 * {@code @EnableMethodSecurity} but is excluded in the test profile via
 * {@code @Profile("!test")}. Importing this class restores method security for controller
 * slice tests without pulling in the full security filter chain.</p>
 */
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {
}

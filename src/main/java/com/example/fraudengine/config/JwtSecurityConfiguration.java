package com.example.fraudengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds JWT role names injected from configuration.
 *
 * <p>Controller methods reference this bean via SpEL in {@code @PreAuthorize}:
 * <pre>
 *   {@literal @}PreAuthorize("hasRole(@jwtSecurityConfiguration.getReadRole())")
 * </pre>
 */
@Component("jwtSecurityConfiguration")
public class JwtSecurityConfiguration {

    @Value("${jwt-configuration.read-role}")
    private String readRole;

    @Value("${jwt-configuration.write-role}")
    private String writeRole;

    public String getReadRole() {
        return readRole;
    }

    public String getWriteRole() {
        return writeRole;
    }
}

package com.example.fraudengine.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.fraudengine.domain.FraudFlag;
import com.example.fraudengine.service.FraudFlagService;
import com.example.fraudengine.web.dto.FraudFlagDetailResponse;
import com.example.fraudengine.web.dto.FraudFlagResponse;
import com.example.fraudengine.web.mapper.FraudFlagMapper;

/**
 * REST controller for fraud flag operations.
 *
 * <p>Read operations require {@code FraudEngineRead} role (or {@code FraudEngineWrite},
 * since write role includes read access). The role names are resolved via
 * {@link com.example.fraudengine.config.JwtSecurityConfiguration} to avoid
 * hardcoding string literals.</p>
 */
@RestController
@RequestMapping("/api/v1/fraud-flags")
@RequiredArgsConstructor
public class FraudFlagController {

    private final FraudFlagService fraudFlagService;
    private final FraudFlagMapper fraudFlagMapper;

    /**
     * Returns a paginated list of fraud flags, optionally filtered by status.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole(@jwtSecurityConfiguration.getReadRole(), @jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<Page<FraudFlagResponse>> listFlags(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        Page<FraudFlagResponse> page = fraudFlagService.findAll(pageable)
                .map(fraudFlagMapper::toResponse);
        return ResponseEntity.ok(page);
    }

    /**
     * Returns the full detail of a single fraud flag, including associated transaction summary.
     */
    @GetMapping("/{flagId}")
    @PreAuthorize("hasAnyRole(@jwtSecurityConfiguration.getReadRole(), @jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<FraudFlagDetailResponse> getFlag(@PathVariable Long flagId) {
        FraudFlag flag = fraudFlagService.findById(flagId);
        return ResponseEntity.ok(fraudFlagMapper.toDetailResponse(flag));
    }
}

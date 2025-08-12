package com.harunykt.performance_review.security;

import com.harunykt.performance_review.model.User;
import com.harunykt.performance_review.model.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class JwtService {

    private final JwtEncoder encoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.ttl-minutes:120}")
    private long ttlMinutes;

    public JwtService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public String generate(User user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole() != null ? user.getRole().name() : UserRole.EMPLOYEE.name())
                .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}

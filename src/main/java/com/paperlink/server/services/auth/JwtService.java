package com.paperlink.server.services.auth;

import com.paperlink.server.entities.UserEntity;
import com.paperlink.server.exceptions.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for JWT token generation and validation
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secretKey}")
    private String secretKey;

    @Value("${jwt.accessToken.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refreshToken.expiration}")
    private long refreshTokenExpiration;

    /**
     * Create SecretKey from configured secret
     *
     * @return SecretKey for signing tokens
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate a new access token for a user
     *
     * @param userEntity User entity
     * @return JWT access token
     */
    public String generateAccessToken(UserEntity userEntity) {
        return generateToken(userEntity, accessTokenExpiration, Map.of(
                "tokenType", "access"
        ));
    }

    /**
     * Generate a new refresh token for a user
     *
     * @param userEntity User entity
     * @return JWT refresh token
     */
    public String generateRefreshToken(UserEntity userEntity) {
        return generateToken(userEntity, refreshTokenExpiration, Map.of(
                "tokenType", "refresh"
        ));
    }

    /**
     * Generate a JWT token with custom claims and expiration
     *
     * @param userEntity User entity
     * @param expiration Expiration time in milliseconds
     * @param extraClaims Additional claims to include in the token
     * @return JWT token
     */
    private String generateToken(UserEntity userEntity, long expiration, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userEntity.getId())
                .claim("username", userEntity.getUsername())
                .claim("authorities", userEntity.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * Extract user ID from a JWT token
     *
     * @param token JWT token
     * @return User ID from the token subject
     * @throws AuthenticationException if token is invalid
     */
    public String getUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new AuthenticationException("Expired JWT token", e);
        } catch (MalformedJwtException | SignatureException e) {
            throw new AuthenticationException("Invalid JWT token", e);
        } catch (Exception e) {
            throw new AuthenticationException("Could not process JWT token", e);
        }
    }

    /**
     * Extract all claims from a JWT token
     *
     * @param token JWT token
     * @return Claims from the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate a JWT token
     *
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a token is an access token
     *
     * @param token JWT token
     * @return true if it's an access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a token is a refresh token
     *
     * @param token JWT token
     * @return true if it's a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }
}
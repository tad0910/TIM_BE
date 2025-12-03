package com.tim.appTim.service;

import com.tim.appTim.entity.InvalidatedToken;
import com.tim.appTim.repository.InvalidatedTokenRepository;
import com.tim.appTim.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private InvalidatedTokenRepository invalidatedTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private String token;
    private Claims claims;
    private Date expiryDate;

    @BeforeEach
    void setUp() {
        token = "test-token";
        expiryDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        
        claims = mock(Claims.class);
    }

    @Test
    void logout_WhenTokenIsValid_ShouldInvalidateToken() {
        // Arrange
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-123");
        when(claims.getExpiration()).thenReturn(expiryDate);
        when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.logout(token);

        // Assert
        verify(jwtUtil).isTokenValid(token);
        verify(jwtUtil).getClaims(token);
        verify(claims).getId();
        verify(claims).getExpiration();
        verify(invalidatedTokenRepository).saveAndFlush(any(InvalidatedToken.class));
    }

    @Test
    void logout_WhenTokenIsInvalid_ShouldThrowException() {
        // Arrange
        when(jwtUtil.isTokenValid(token)).thenReturn(false);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.logout(token);
        });
        assertTrue(exception.getMessage().contains("Token không hợp lệ"));
        verify(jwtUtil).isTokenValid(token);
        verify(jwtUtil, never()).getClaims(anyString());
        verify(invalidatedTokenRepository, never()).saveAndFlush(any(InvalidatedToken.class));
    }

    @Test
    void logout_ShouldCreateInvalidatedTokenWithCorrectData() {
        // Arrange
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-123");
        when(claims.getExpiration()).thenReturn(expiryDate);
        when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
                .thenAnswer(invocation -> {
                    InvalidatedToken invalidatedToken = invocation.getArgument(0);
                    assertEquals("jti-123", invalidatedToken.getJti());
                    assertEquals(expiryDate.toInstant(), invalidatedToken.getExpiryDate());
                    return invalidatedToken;
                });

        // Act
        authService.logout(token);

        // Assert
        verify(invalidatedTokenRepository).saveAndFlush(any(InvalidatedToken.class));
    }

    @Test
    void logout_WhenClaimsHasNoJti_ShouldStillWork() {
        // Arrange
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(claims.getId()).thenReturn(null);
        when(claims.getExpiration()).thenReturn(expiryDate);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(invalidatedTokenRepository.saveAndFlush(any(InvalidatedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.logout(token);

        // Assert
        verify(invalidatedTokenRepository).saveAndFlush(any(InvalidatedToken.class));
    }

    @Test
    void logout_WhenExpiryDateIsNull_ShouldHandleGracefully() {
        // Arrange
        when(jwtUtil.isTokenValid(token)).thenReturn(true);
        when(claims.getExpiration()).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);

        // Act & Assert
        // Should throw NullPointerException when trying to convert null to Instant
        assertThrows(Exception.class, () -> {
            authService.logout(token);
        });
        verify(jwtUtil).isTokenValid(token);
        verify(jwtUtil).getClaims(token);
        verify(claims).getExpiration();
    }
}


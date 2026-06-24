package com.tim.appTim.service;

import com.tim.appTim.dto.request.*;
import com.tim.appTim.dto.response.*;
import com.tim.appTim.dto.common.*;

import com.tim.appTim.entity.PasswordResetRequest;
import com.tim.appTim.entity.PasswordResetRequest.TokenType;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.PasswordResetRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetRequestRepository resetRepo;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private final String email = "test@example.com";
    private final String pepper = "test-pepper";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "pepper", pepper);
        user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setUsername("testuser");
    }

    @Test
    void requestReset_Success() {
        when(userService.findByUsernameOrEmail(email)).thenReturn(user);

        passwordResetService.requestReset(email, "127.0.0.1", "Mozilla");

        ArgumentCaptor<PasswordResetRequest> captor = ArgumentCaptor.forClass(PasswordResetRequest.class);
        verify(resetRepo).save(captor.capture());
        PasswordResetRequest savedRequest = captor.getValue();
        assertThat(savedRequest.getUser()).isEqualTo(user);
        assertThat(savedRequest.getTokenType()).isEqualTo(TokenType.OTP);
        assertThat(savedRequest.getRequestIp()).isEqualTo("127.0.0.1");
        assertThat(savedRequest.getUserAgent()).isEqualTo("Mozilla");
        assertThat(savedRequest.getExpiresAt()).isAfter(Instant.now());

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void requestReset_UserNotFound_ShouldDoNothing() {
        when(userService.findByUsernameOrEmail(email)).thenReturn(null);

        passwordResetService.requestReset(email, "127.0.0.1", "Mozilla");

        verify(resetRepo, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void verifyOtp_Success() {
        String otp = "123456";
        // We need to simulate the hashing logic to match expectations if we want to be
        // precise,
        // or just rely on the service logic. Since hashOtp is private, we can't call it
        // directly easily.
        // But verifyOtp calls hashOtp internally.
        // We need to set up a request with a hash that matches "123456" + pepper.
        // Alternatively, we can just verify that logic proceeds if we mock the repo
        // correctly.
        // Wait, verifyOtp calculates hash of input OTP and compares with stored hash.
        // So we need to store a hash that corresponds to "123456".
        // Since we can't easily generate the exact hash without duplicating logic,
        // let's rely on the fact that we can't easily mock private methods.
        // We will use Reflection to invoke hashOtp or just duplicate the simple HMAC
        // logic here for test setup.

        // Actually, better approach: The service calculates hash of input.
        // So if we pass "123456", service calculates hash("123456").
        // We need the stored request to have THAT same hash.
        // We can use ReflectionTestUtils to invoke the private hashOtp method to
        // generate the expected hash.
        String expectedHash = ReflectionTestUtils.invokeMethod(passwordResetService, "hashOtp", otp);

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUser(user);
        request.setOtpHash(expectedHash);
        request.setTokenType(TokenType.OTP);
        request.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        request.setUsed(false);

        when(userService.findByUsernameOrEmail(email)).thenReturn(user);
        when(resetRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(request));
        when(passwordEncoder.encode(anyString())).thenReturn("token-hash");

        String token = passwordResetService.verifyOtp(email, otp);

        assertThat(token).isNotNull();
        assertThat(request.isUsed()).isTrue();

        // Verify new token request is saved
        verify(resetRepo, times(2)).save(any(PasswordResetRequest.class)); // 1 for update used, 1 for new token
    }

    @Test
    void verifyOtp_InvalidOtp_ShouldThrowException() {
        String otp = "123456";
        String wrongOtp = "654321";
        String expectedHash = ReflectionTestUtils.invokeMethod(passwordResetService, "hashOtp", otp);

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUser(user);
        request.setOtpHash(expectedHash);
        request.setTokenType(TokenType.OTP);
        request.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        request.setAttempts(0);

        when(userService.findByUsernameOrEmail(email)).thenReturn(user);
        when(resetRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> passwordResetService.verifyOtp(email, wrongOtp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid OTP");

        assertThat(request.getAttempts()).isEqualTo(1);
        verify(resetRepo).save(request);
    }

    @Test
    void verifyOtp_Expired_ShouldThrowException() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));

        when(userService.findByUsernameOrEmail(email)).thenReturn(user);
        when(resetRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> passwordResetService.verifyOtp(email, "any"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OTP expired. Please request a new one.");
    }

    @Test
    void resetPassword_Success() {
        String resetToken = "valid-token";
        String newPassword = "newPassword123";
        String tokenHash = "hashed-token";

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUser(user);
        request.setTokenType(TokenType.LINK);
        request.setOtpHash(tokenHash); // In LINK mode, otpHash stores the token hash
        request.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        request.setUsed(false);

        when(userService.findByUsernameOrEmail(email)).thenReturn(user);
        when(resetRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(request));
        when(passwordEncoder.matches(resetToken, tokenHash)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded-new-password");

        passwordResetService.resetPassword(email, resetToken, newPassword);

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getPasswordChangedAt()).isNotNull();
        assertThat(request.isUsed()).isTrue();

        verify(userService).internalSave(user);
        verify(resetRepo).save(request);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void resetPassword_InvalidToken_ShouldThrowException() {
        String resetToken = "invalid-token";
        String tokenHash = "hashed-token";

        PasswordResetRequest request = new PasswordResetRequest();
        request.setUser(user);
        request.setTokenType(TokenType.LINK);
        request.setOtpHash(tokenHash);
        request.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userService.findByUsernameOrEmail(email)).thenReturn(user);
        when(resetRepo.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user)).thenReturn(Optional.of(request));
        when(passwordEncoder.matches(resetToken, tokenHash)).thenReturn(false);

        assertThatThrownBy(() -> passwordResetService.resetPassword(email, resetToken, "newPass"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid reset token");
    }
}


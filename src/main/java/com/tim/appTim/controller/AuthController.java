package com.tim.appTim.controller; // Đảm bảo package đúng với cấu trúc dự án
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tim.appTim.dto.LoginRequest;
import com.tim.appTim.dto.LoginResponse;
import com.tim.appTim.entity.User;
import com.tim.appTim.service.UserService;
import com.tim.appTim.util.JwtUtil;
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AuthenticationManager authenticationManager;
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        userService.register(user);
        return ResponseEntity.ok("User registered successfully");
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(), loginRequest.getPassword()
                )
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            String token = jwtUtil.generateToken(loginRequest.getEmail());
            return ResponseEntity.ok(new LoginResponse(token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Login failed: " + e.getMessage());
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // JWT stateless → chỉ cần client xóa token
        return ResponseEntity.ok("Logout successful. Please remove token from client.");
    }
}

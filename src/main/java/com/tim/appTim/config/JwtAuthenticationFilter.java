    package com.tim.appTim.config;

    import com.tim.appTim.repository.InvalidatedTokenRepository;
    import com.tim.appTim.util.JwtUtil;
    import io.jsonwebtoken.Claims;
    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.lang.NonNull;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
    import org.springframework.stereotype.Component;
    import org.springframework.web.filter.OncePerRequestFilter;

    import java.io.IOException;

    @Component
    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;
        private final UserDetailsService userDetailsService;
        private final InvalidatedTokenRepository invalidatedTokenRepository;

        public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, InvalidatedTokenRepository invalidatedTokenRepository) {
            this.jwtUtil = jwtUtil;
            this.userDetailsService = userDetailsService;
            this.invalidatedTokenRepository = invalidatedTokenRepository;
        }

        @Override
        protected void doFilterInternal(
                @NonNull HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain) throws ServletException, IOException {

            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String token = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.getClaims(token);

                // 🔸 1. Nếu token có "iss" chứa "keycloak" → bỏ qua để OAuth2ResourceServer xử lý
                String issuer = claims.getIssuer();
                if (issuer != null && issuer.toLowerCase().contains("keycloak")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 🔸 2. Nếu token của bạn nằm trong danh sách blacklist → từ chối
                String jti = claims.getId();
                if (invalidatedTokenRepository.existsByJti(jti)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token đã bị vô hiệu hóa (đã đăng xuất).");
                    return;
                }

                // 🔸 3. Giải mã token cục bộ
                final String usernameOrEmail = jwtUtil.extractUsernameOrEmail(token);
                if (usernameOrEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(usernameOrEmail);

                    if (jwtUtil.isTokenValid(token)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }

            } catch (Exception e) {
                // Nếu token bị lỗi format (ví dụ token Keycloak) → bỏ qua để OAuth2ResourceServer xử lý
                filterChain.doFilter(request, response);
                return;
            }

            filterChain.doFilter(request, response);
        }
    }
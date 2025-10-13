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

            // Nếu không có header Authorization hoặc không phải Bearer token, bỏ qua
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String token = authHeader.substring(7);

            // Trước khi giải mã, kiểm tra xem token có trong blacklist không
            // Lấy jti từ token
            try {
                String jti = jwtUtil.getClaims(token).getId();
                System.out.println("FILTER: Đang kiểm tra token với JTI = " + jti);
                boolean isInvalidated = invalidatedTokenRepository.existsByJti(jti);
                System.out.println("FILTER: Token này có trong sổ đen không? -> " + isInvalidated);
                if (invalidatedTokenRepository.existsByJti(jti)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Token đã bị vô hiệu hóa (đã đăng xuất).");
                    return; // Dừng lại ngay lập tức
                }
            } catch (Exception e) {
                // Lỗi khi parse token (có thể đã hết hạn hoặc không hợp lệ)
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Token không hợp lệ.");
                return;
            }


            final String usernameOrEmail = jwtUtil.extractUsernameOrEmail(token);

            // Nếu có username và chưa được xác thực trong context
            if (usernameOrEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(usernameOrEmail);

                // Kiểm tra token có hợp lệ không (dựa trên hàm isTokenValid của bạn)
                if (jwtUtil.isTokenValid(token)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Cập nhật SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // Chuyển request cho filter tiếp theo trong chuỗi
            filterChain.doFilter(request, response);
        }
    }
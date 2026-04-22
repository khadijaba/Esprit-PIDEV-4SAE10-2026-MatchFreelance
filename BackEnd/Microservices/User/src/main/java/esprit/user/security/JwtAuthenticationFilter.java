package esprit.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        String email = jwtUtil.getEmailFromToken(token);
        String role = jwtUtil.getRoleFromToken(token).name();

        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        var auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(email, "", authorities),
                null,
                authorities
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        request.setAttribute("userId", userId);
        request.setAttribute("userRole", role);
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}

package com.alfre.DHHotel.adapter.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * This class implements a JWT authentication filter that validates JSON Web Tokens (JWT)
 * for incoming HTTP requests. If a valid token is found, it sets the authentication
 * in the security context.
 * <p>
 * This filter extends OncePerRequestFilter to guarantee that it is executed only once per request.
 *
 * @author Alfredo Sobrados González
 */
@Component
@RequiredArgsConstructor
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Filters incoming HTTP requests and sets up the security context if a valid JWT is found.
     * This method extracts the token from the request header, validates it, and then loads
     * the corresponding user details. If the token is valid, an authentication object is set in
     * the SecurityContext.
     *
     * @param request the HttpServletRequest being processed
     * @param response the HttpServletResponse associated with the request
     * @param filterChain the FilterChain to pass the request and response to the next filter
     * @throws ServletException if a servlet error occurs during processing
     * @throws IOException if an I/O error occurs during processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String token = getTokenFromRequest(request);
        final String username;

        // If no token is provided, continue the filter chain without setting authentication
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the username from the JWT
        username = jwtService.getUsernameFromToken(token);

        // If a username is found and the security context does not have an authentication yet
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate the token against the user details
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the security context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the HTTP Authorization header of the request.
     * The header must start with "Bearer " to be considered a valid token header.
     *
     * @param request the HttpServletRequest from which to extract the token
     * @return the JWT string if present and valid, otherwise null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
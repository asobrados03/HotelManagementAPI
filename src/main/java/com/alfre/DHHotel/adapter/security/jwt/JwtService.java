package com.alfre.DHHotel.adapter.security.jwt;

import com.alfre.DHHotel.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides services for generating and validating JSON Web Tokens (JWT).
 * This class handles token creation by embedding claims such as userId and roles,
 * and offers methods to extract information from and validate JWTs.
 * <p>
 * The token expiration is set to 24 hours.
 *
 * @author Alfredo Sobrados González
 */
@Service
public class JwtService {
    private static final String SECRET_KEY = "413F4428472B4B6250655368566D5970337336763979244226452948404D6351";

    /**
     * Generates a JWT for the given user.
     * The token includes custom claims such as the userId (if available) and the user's roles.
     *
     * @param user the UserDetails object representing the authenticated user
     * @return a signed JWT as a String
     */
    public String getToken(UserDetails user) {
        Map<String, Object> claims = new HashMap<>();

        // Add userId to the token claims if the user object is an instance of User
        if (user instanceof User) {
            claims.put("userId", ((User) user).id);
        }

        // Add the user's roles to the token claims
        claims.put("roles", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return getToken(claims, user);
    }

    /**
     * Builds and signs a JWT using the provided claims and user details.
     * The token is set to expire in 24 hours from the time of creation.
     *
     * @param extraClaims additional claims to be included in the token
     * @param user the UserDetails object used for setting the subject of the token
     * @return a signed JWT as a String
     */
    private String getToken(Map<String, Object> extraClaims, UserDetails user) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(user.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24 hours validity
                .signWith(getKey())
                .compact();
    }

    /**
     * Retrieves the secret key used for signing the JWT.
     *
     * @return a SecretKey derived from the Base64-encoded SECRET_KEY
     */
    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extracts the username (subject) from the given JWT.
     *
     * @param token the JWT from which to extract the username
     * @return the username contained in the token
     */
    public String getUsernameFromToken(String token) {
        return getClaim(token, Claims::getSubject);
    }

    /**
     * Validates the provided JWT against the given user details.
     * The token is considered valid if the username in the token matches
     * the user details and the token has not expired.
     *
     * @param token the JWT to validate
     * @param userDetails the UserDetails to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Retrieves all claims present in the given JWT.
     *
     * @param token the JWT from which to extract claims
     * @return a Claims object containing all claims from the token
     */
    private Claims getAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts a specific claim from the JWT using a resolver function.
     *
     * @param <T> the type of the claim
     * @param token the JWT from which to extract the claim
     * @param claimsResolver a function to extract the desired claim from the Claims object
     * @return the extracted claim of type T
     */
    public <T> T getClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts the expiration date of the JWT.
     *
     * @param token the JWT from which to extract the expiration date
     * @return a Date representing the token's expiration time
     */
    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    /**
     * Determines whether the given JWT has expired.
     *
     * @param token the JWT to check for expiration
     * @return true if the token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }
}
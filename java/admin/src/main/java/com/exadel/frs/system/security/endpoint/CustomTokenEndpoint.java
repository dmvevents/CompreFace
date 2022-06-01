package com.exadel.frs.system.security.endpoint;

import static com.exadel.frs.system.global.Constants.ACCESS_TOKEN_COOKIE_NAME;
import static com.exadel.frs.system.global.Constants.REFRESH_TOKEN_COOKIE_NAME;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import com.exadel.frs.commonservice.entity.User;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping(value = "/oauth/token")
public class CustomTokenEndpoint extends TokenEndpoint {

    @Autowired
    private HttpServletRequest currentRequest;

    @PostMapping
    public ResponseEntity<OAuth2AccessToken> postAccessToken(
            Principal principal,
            @RequestParam
            Map<String, String> parameters
    ) throws HttpRequestMethodNotSupportedException {

        if (principal instanceof UsernamePasswordAuthenticationToken) {
            if (((UsernamePasswordAuthenticationToken) principal).getPrincipal() instanceof User) {
                return ResponseEntity.status(HttpStatus.OK).build();
            }
        }

        if (isRefreshTokenRequest(parameters)) {
            val refreshTokenValue = extractRefreshTokenCookieValueFromRequest(currentRequest);
            parameters.put("refresh_token", refreshTokenValue);
        }

        val tokenResponse = super.postAccessToken(principal, parameters);

        val accessToken = tokenResponse.getBody();
        val refreshToken = (DefaultExpiringOAuth2RefreshToken) accessToken.getRefreshToken();

        val accessTokenCookie = buildAccessTokenCookie(accessToken.getValue(), accessToken.getExpiresIn());
        val refreshTokenCookie = buildRefreshTokenCookie(refreshToken.getValue(), refreshToken.getExpiration().getTime());

        val headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessTokenCookie);
        headers.add(HttpHeaders.SET_COOKIE, refreshTokenCookie);

        return ResponseEntity.status(HttpStatus.OK).headers(headers).build();
    }

    private String extractRefreshTokenCookieValueFromRequest(final HttpServletRequest request) {
        return Arrays.stream(request.getCookies())
                     .filter(cookie -> cookie.getName().equals(REFRESH_TOKEN_COOKIE_NAME))
                     .map(Cookie::getValue)
                     .findFirst()
                     .orElse(EMPTY);
    }

    private boolean isRefreshTokenRequest(final Map<String, String> requestParams) {
        return "refresh_token".equals(requestParams.get("grant_type"));
    }

    private String buildAccessTokenCookie(final String value, final long expiresIn) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, value)
                             .httpOnly(true)
                             .maxAge(expiresIn)
                             .path("/admin")
                             .build()
                             .toString();
    }

    private String buildRefreshTokenCookie(final String value, final long expiresIn) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                             .httpOnly(true)
                             .maxAge(expiresIn)
                             .path("/oauth/token")
                             .build()
                             .toString();
    }
}

package com.unifina.security;

import com.unifina.domain.security.SecUser;
import com.unifina.service.PermissionService;

import javax.servlet.http.HttpServletRequest;

public class TokenAuthenticator {
	private final PermissionService permissionService;
	private boolean lastAuthenticationMalformed = false;

	public TokenAuthenticator(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public SecUser authenticate(HttpServletRequest request) {
		String apiKey = parseAuthorizationHeader(request.getHeader("Authorization"));
		return permissionService.getUserByApiKey(apiKey);
	}

	public boolean lastAuthenticationMalformed() {
		return lastAuthenticationMalformed;
	}

	/**
	 * "Authorization: Token apiKey" => "apiKey"
	 */
	private String parseAuthorizationHeader(String s) {
		s = s == null ? null : s.trim();
		if (s != null && !s.isEmpty()) {
			String[] parts = s.split("\\s+");
			if (parts.length == 2 && parts[0].toLowerCase().equals("token")) {
				lastAuthenticationMalformed = false;
				return parts[1];
			} else {
				lastAuthenticationMalformed = true;
			}
 		}
		return null;
	}
}

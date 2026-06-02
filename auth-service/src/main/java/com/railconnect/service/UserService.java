package com.railconnect.service;

import com.railconnect.dto.request.RegisterRequest;
import com.railconnect.dto.response.UserResponse;
import com.railconnect.entity.User;
import com.railconnect.security.oauth2.OAuth2UserInfo;

public interface UserService {
    User findOrCreateOAuth2User(OAuth2UserInfo userInfo, String provider);
    UserResponse getUserProfile(String email);
    void updateProfile(String email, RegisterRequest request);
}

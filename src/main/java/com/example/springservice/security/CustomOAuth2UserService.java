package com.example.springservice.security;

import com.example.springservice.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        GoogleUserInfo googleUserInfo = GoogleUserInfo.fromAttributes(oAuth2User.getAttributes());
        if (googleUserInfo.subject() == null || googleUserInfo.subject().isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Google 사용자 식별자가 없습니다.", "OAUTH_SUBJECT_MISSING");
        }
        return oAuth2User;
    }
}

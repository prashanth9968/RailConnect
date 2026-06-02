package com.railconnect.security.oauth2;
import com.railconnect.entity.User;
import com.railconnect.enums.AuthProvider;
import com.railconnect.enums.Role;
import com.railconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service @Slf4j @RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        OAuth2User oAuth2User = super.loadUser(req);
        Map<String, Object> attrs = oAuth2User.getAttributes();
        String providerId = (String) attrs.get("sub");
        String email = (String) attrs.get("email");
        String firstName = (String) attrs.getOrDefault("given_name", "User");
        String lastName = (String) attrs.getOrDefault("family_name", "");
        String picture = (String) attrs.get("picture");
        User user = userRepository.findByProviderIdAndAuthProvider(providerId, AuthProvider.GOOGLE)
            .orElseGet(() -> userRepository.findByEmail(email).orElse(null));
        if (user == null) {
            user = User.builder()
                .email(email).firstName(firstName).lastName(lastName)
                .profilePictureUrl(picture).authProvider(AuthProvider.GOOGLE)
                .providerId(providerId).emailVerified(true).role(Role.PASSENGER)
                .build();
        } else {
            user.setFirstName(firstName); user.setLastName(lastName);
            user.setProfilePictureUrl(picture); user.setProviderId(providerId);
            user.setAuthProvider(AuthProvider.GOOGLE);
        }
        userRepository.save(user);
        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attrs, "sub");
    }
}

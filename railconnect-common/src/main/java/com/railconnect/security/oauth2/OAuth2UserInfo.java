package com.railconnect.security.oauth2;

import java.util.Map;

public class OAuth2UserInfo {
    private final Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getId() { return (String) attributes.get("sub"); }
    public String getName() { return (String) attributes.get("name"); }
    public String getEmail() { return (String) attributes.get("email"); }
    public String getPicture() { return (String) attributes.get("picture"); }
    public Map<String, Object> getAttributes() { return attributes; }
}

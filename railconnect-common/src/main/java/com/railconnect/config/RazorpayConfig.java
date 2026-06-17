package com.railconnect.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key-id:rzp_test_mock}")
    private String keyId;

    @Value("${razorpay.key-secret:mock_secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(keyId, keySecret);
    }
}

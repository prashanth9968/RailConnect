package com.railconnect.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PassengerRequest {
    @NotBlank private String name;
    @NotNull @Min(1) @Max(120) private Integer age;
    @NotBlank private String gender;
    private String idType;
    private String idNumber;
    private String berthPreference;
}

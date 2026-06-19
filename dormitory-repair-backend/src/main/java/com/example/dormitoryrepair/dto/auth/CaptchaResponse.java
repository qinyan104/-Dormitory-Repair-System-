package com.example.dormitoryrepair.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaResponse {

    private String captchaKey;
    private String captchaImage;
}

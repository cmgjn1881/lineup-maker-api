package com.lineupmaker.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeVerificationRequest {
    private String email;
    private String verificationCode;
}

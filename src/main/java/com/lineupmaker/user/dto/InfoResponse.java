package com.lineupmaker.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InfoResponse {
    private String username;
    private String email;
}

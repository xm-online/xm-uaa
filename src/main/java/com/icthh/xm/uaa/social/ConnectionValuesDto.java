package com.icthh.xm.uaa.social;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionValuesDto {
    private String providerUserId;
    private String displayName;
    private String profileUrl;
    private String imageUrl;
}

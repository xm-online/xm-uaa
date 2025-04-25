package com.icthh.xm.uaa.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class PermissionContextDto {

    private List<String> permissions;
    private Map<String, Object> ctx;
    private String hash;

    public PermissionContextDto() {
        this.permissions = List.of();
        this.ctx = Map.of();
    }
}

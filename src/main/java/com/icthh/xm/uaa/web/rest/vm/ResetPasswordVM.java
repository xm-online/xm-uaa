package com.icthh.xm.uaa.web.rest.vm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordVM {
    @NotNull String login;
    @NotNull String resetType;
    @NotNull String loginType;
}

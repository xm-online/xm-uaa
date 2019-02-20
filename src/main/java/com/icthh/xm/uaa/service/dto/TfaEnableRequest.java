package com.icthh.xm.uaa.service.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * The {@link TfaEnableRequest} class.
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TfaEnableRequest {

    @Valid
    @NotNull
    private TfaOtpChannelSpec otpChannelSpec;

}

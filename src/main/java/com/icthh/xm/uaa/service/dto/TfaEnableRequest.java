package com.icthh.xm.uaa.service.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The {@link TfaEnableRequest} class.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TfaEnableRequest {

    @Valid
    @NotNull
    private TfaOtpChannelSpec otpChannelSpec;

}

package com.icthh.xm.uaa.service.dto;

import com.icthh.xm.uaa.domain.OtpChannelType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The {@link TfaOtpChannelSpec} class.
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class TfaOtpChannelSpec {

    @NotNull
    private OtpChannelType channelType;

    @NotBlank
    private String destination;

}

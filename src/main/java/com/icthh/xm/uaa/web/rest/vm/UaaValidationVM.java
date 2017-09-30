package com.icthh.xm.uaa.web.rest.vm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UaaValidationVM {

    private boolean isValid;
    private String errorMessage;

}

package com.icthh.xm.uaa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSpec {

    private String roleKey;
    private String dataSpec;
    private String dataForm;

}

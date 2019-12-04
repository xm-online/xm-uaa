package com.icthh.xm.uaa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class TemplateParams {
    private List<Object> templateParams = new ArrayList<>();
}

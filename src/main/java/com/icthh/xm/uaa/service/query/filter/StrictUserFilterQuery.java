package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.filter.StringFilter;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class StrictUserFilterQuery extends AbstractUserFilterQuery implements Serializable {
    private StringFilter firstName;
    private StringFilter lastName;
    private StringFilter login;
    private StringFilter authority;
    private Map<String, String> dataAttributes = new HashMap<>() {{
        put("salesPointId","1000101106");
    }};
}

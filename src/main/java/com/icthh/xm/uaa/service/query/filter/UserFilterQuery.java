package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.filter.StringFilter;
import java.io.Serializable;
import lombok.Data;

@Data
public class UserFilterQuery implements Serializable {
    private StringFilter firstName;
    private StringFilter lastName;
    private StringFilter login;
}

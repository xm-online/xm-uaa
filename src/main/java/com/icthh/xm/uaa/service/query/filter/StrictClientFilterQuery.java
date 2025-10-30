package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.filter.StringFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class StrictClientFilterQuery implements Criteria, Serializable {

    private StringFilter clientId;
    private StringFilter roleKey;
    private StringFilter description;
    private StringFilter state;
    private StringFilter scopes;

    @Override
    public Criteria copy() {
        return this.toBuilder().build();
    }
}

package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.filter.StringFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class StrictUserFilterQuery extends AbstractUserFilterQuery implements Criteria, Serializable {
    private StringFilter firstName;
    private StringFilter lastName;
    private StringFilter login;
    private StringFilter authority;
    private List<DataAttributeCriteria> dataAttributes = new ArrayList<>();

    @Override
    public Criteria copy() {
        List<DataAttributeCriteria> dataAttributesCriterion = this.dataAttributes.stream()
            .map(DataAttributeCriteria::copy)
            .collect(Collectors.toList());

        return this.toBuilder()
            .dataAttributes(dataAttributesCriterion)
            .build();
    }
}

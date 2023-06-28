package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.filter.StringFilter;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class StrictUserFilterQuery extends AbstractUserFilterQuery implements Criteria, Serializable {
    private StringFilter firstName;
    private StringFilter lastName;
    private StringFilter login;
    private StringFilter authority;
    private List<DataAttributeCriteria> dataAttributes;

    @Override
    public Criteria copy() {
        log.info("DataAttributeCriteria: {}", dataAttributes);
        List<DataAttributeCriteria> dataAttributesCriterion = this.dataAttributes.stream()
            .map(DataAttributeCriteria::copy)
            .collect(Collectors.toList());

        return this.toBuilder()
            .dataAttributes(dataAttributesCriterion)
            .build();
    }
}

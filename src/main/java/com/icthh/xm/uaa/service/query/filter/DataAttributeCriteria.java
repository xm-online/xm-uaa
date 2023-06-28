package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.StringFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DataAttributeCriteria implements Criteria {

    private StringFilter path;
    private String pathS;
    private Filter<String> type;

    @Override
    public DataAttributeCriteria copy() {
        return this.toBuilder().build();
    }
}

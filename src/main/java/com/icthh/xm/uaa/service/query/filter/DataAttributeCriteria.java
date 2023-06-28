package com.icthh.xm.uaa.service.query.filter;

import io.github.jhipster.service.Criteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DataAttributeCriteria implements Criteria {

    private String path;
    private String value;
    private String operation;

    @Override
    public DataAttributeCriteria copy() {
        return this.toBuilder().build();
    }
}

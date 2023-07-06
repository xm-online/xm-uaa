package com.icthh.xm.uaa.service.query.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.jhipster.service.Criteria;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class DataAttributeCriteria implements Criteria {

    private String path;
    private String value;
    private OPERATION operation;

    @Override
    public DataAttributeCriteria copy() {
        return this.toBuilder().build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public enum OPERATION {
        @JsonProperty("equals")
        EQUALS,
        @JsonProperty("contains")
        CONTAINS;
    }
}

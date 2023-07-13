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
    private Operation operation;
    private ValueType type;

    @Override
    public DataAttributeCriteria copy() {
        return this.toBuilder().build();
    }

    @Getter
    @AllArgsConstructor
    public enum Operation {
        @JsonProperty("equals")
        EQUALS("equals"),
        @JsonProperty("contains")
        CONTAINS("contains");

        private final String operation;

        public static Operation fromOperation(String o) {
            for (Operation operation : Operation.values()) {
                if (operation.getOperation().equalsIgnoreCase(o)) {
                    return operation;
                }
            }
            throw new IllegalArgumentException("Invalid operation: " + o);
        }
    }

    @Getter
    @AllArgsConstructor
    public enum ValueType {
        @JsonProperty("number")
        NUMBER("number") ,
        @JsonProperty("string")
        STRING("string"),
        @JsonProperty("boolean")
        BOOLEAN("boolean");

        private final String type;

        public static ValueType fromValueType(String type) {
            for (ValueType valueType : ValueType.values()) {
                if (valueType.getType().equalsIgnoreCase(type)) {
                    return valueType;
                }
            }
            return ValueType.STRING;
        }
    }
}

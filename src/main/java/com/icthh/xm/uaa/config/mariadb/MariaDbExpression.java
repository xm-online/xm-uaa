package com.icthh.xm.uaa.config.mariadb;

import com.icthh.xm.commons.migration.db.jsonb.CustomExpression;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

@Component
@ConditionalOnExpression("'${spring.datasource.url}'.startsWith('jdbc:mariadb:')")
public class MariaDbExpression implements CustomExpression {

    public MariaDbExpression() {
        System.out.println("MariaDbExpression");
    }

    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        throw new NotImplementedException("Not implemented yet");
    }

    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    public Expression<?> toExpression(CriteriaBuilder cb, Object object) {
        throw new NotImplementedException("Not implemented yet");
    }

    public Expression<?> toJsonB(CriteriaBuilder cb, Object object) {
        throw new NotImplementedException("Not implemented yet");
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        throw new NotImplementedException("Not implemented yet");
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        throw new NotImplementedException("Not implemented yet");
    }
}


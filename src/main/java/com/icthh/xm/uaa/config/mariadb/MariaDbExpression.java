package com.icthh.xm.uaa.config.mariadb;

import com.icthh.xm.commons.migration.db.jsonb.CustomExpression;
import com.icthh.xm.commons.migration.db.jsonb.HibernateInlineExpression;
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
    }

    public Expression<JsonBinaryType> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath) {
        return this.jsonQuery(cb, root, column, jsonPath, JsonBinaryType.class);
    }

    public <T> Expression<T> jsonQuery(CriteriaBuilder cb, Root<?> root, String column, String jsonPath, Class<T> type) {
        return cb.function("JSON_VALUE", type, new Expression[]{root.get(column), new HibernateInlineExpression(cb, jsonPath)});
    }

    public Expression<?> toExpression(CriteriaBuilder cb, Object object) {
        return this.toJsonB(cb, object);
    }

    public Expression<?> toJsonB(CriteriaBuilder cb, Object object) {
        return this.toJsonB(cb, object, String.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Object object, Class<T> type) {
        return this.toJsonB(cb, cb.literal(object), type);
    }

    public Expression<JsonBinaryType> toJsonB(CriteriaBuilder cb, Expression<?> expression) {
        return this.toJsonB(cb, expression, JsonBinaryType.class);
    }

    public <T> Expression<T> toJsonB(CriteriaBuilder cb, Expression<?> expression, Class<T> type) {
        return cb.function("JSON_COMPACT", type, expression);
    }
}


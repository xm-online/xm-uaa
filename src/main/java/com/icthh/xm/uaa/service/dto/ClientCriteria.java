package com.icthh.xm.uaa.service.dto;

import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.filter.Filter;
import io.github.jhipster.service.filter.LongFilter;
import io.github.jhipster.service.filter.StringFilter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Objects;


/**
 * Criteria class for the {@link com.icthh.xm.uaa.domain.Client} entity. This class is used
 * in {@link com.icthh.xm.uaa.web.rest.ClientResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /clients?clientId.contains=jhon&id.greaterThan=5}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class ClientCriteria implements Serializable, Criteria {

    private LongFilter id;
    private StringFilter clientId;
    private StringFilter roleKey;

    public ClientCriteria(ClientCriteria other) {
        this.id = other.id == null ? null : other.id.copy();
        this.clientId = other.clientId == null ? null : other.clientId.copy();
        this.roleKey = other.roleKey == null ? null : other.roleKey.copy();
    }


    @Override
    public Criteria copy() {
        return new ClientCriteria(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ClientCriteria that = (ClientCriteria) o;
        return
            Objects.equals(id, that.id) &&
                Objects.equals(clientId, that.clientId) &&
                Objects.equals(roleKey, that.roleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId, roleKey);
    }
}

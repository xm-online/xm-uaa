package com.icthh.xm.uaa.service.query;

import com.icthh.xm.commons.migration.db.jsonb.CustomExpression;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLogin_;
import com.icthh.xm.uaa.domain.User_;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria;
import com.icthh.xm.uaa.service.query.filter.SoftUserFilterQuery;
import com.icthh.xm.uaa.service.query.filter.StrictUserFilterQuery;
import io.github.jhipster.service.Criteria;
import io.github.jhipster.service.QueryService;
import io.github.jhipster.service.filter.StringFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation.CONTAINS;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.Operation.EQUALS;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType.BOOLEAN;
import static com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria.ValueType.NUMBER;
import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService extends QueryService<User> {

    private final UserRepository userRepository;
    private final CustomExpression customExpression;

    public Page<UserDTO> findAllUsersByStrictMatch(StrictUserFilterQuery filterQuery, Pageable pageable) {
        Specification<User> specification = createStrictSpecification(filterQuery);
        return userRepository.findAll(specification, pageable).map(UserDTO::new);
    }

    public Page<UserDTO> findAllUsersBySoftMatch(SoftUserFilterQuery query, Pageable pageable) {
        Specification<User> specification = createSoftSpecification(query);
        return userRepository.findAll(specification, pageable).map(UserDTO::new);
    }

    private Specification<User> createStrictSpecification(StrictUserFilterQuery filterQuery) {
        return createStrictSpecs(filterQuery)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Specification.where(null), Specification::and);
    }

    private Specification<User> createSoftSpecification(SoftUserFilterQuery query) {
        return Stream.of(
                ofNullable(query.getRoleKey()).map(fn -> buildStringSpecification(fn, User_.roleKey)),
                ofNullable(query.getActivated()).map(fn -> buildSpecification(fn, User_.activated))
            )
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Specification.where(
                getLoginSpecificationForSoft(query.getQuery())
                    .or(buildStringSpecification(query.getQuery(), User_.lastName))
                    .or(buildStringSpecification(query.getQuery(), User_.firstName))
                    .or(buildSpecification(query.getQuery(), root -> root.get(User_.AUTHORITIES).as(String.class)))
            ), Specification::and);
    }

    private Stream<Optional<Specification<User>>> createStrictSpecs(StrictUserFilterQuery filterQuery) {
        Stream<Optional<Specification<User>>> filters = Stream.of(
            ofNullable(filterQuery.getLogin()).map(this::getLoginSpecificationForStrict),
            ofNullable(filterQuery.getLastName()).map(ln -> buildStringSpecification(ln, User_.lastName)),
            ofNullable(filterQuery.getFirstName()).map(fn -> buildStringSpecification(fn, User_.firstName)),
            ofNullable(filterQuery.getRoleKey()).map(fn -> buildStringSpecification(fn, User_.roleKey)),
            ofNullable(filterQuery.getActivated()).map(fn -> buildSpecification(fn, User_.activated)),
            ofNullable(filterQuery.getAuthority()).map(fn -> buildSpecification(fn, root -> root.get(User_.AUTHORITIES).as(String.class)))
        );
        Stream<Optional<Specification<User>>> dataAttributes = buildDataAttributes(filterQuery.getDataAttributes());
        return Stream.concat(filters, dataAttributes);
    }

    private Specification<User> getLoginSpecificationForStrict(StringFilter loginFilter) {
        AtomicReference<CriteriaQuery<?>> queryReference = new AtomicReference<>();
        Consumer<CriteriaQuery<?>> queryConsumer = queryReference::set;

        Function<Root<User>, Join<User, UserLogin>> functionToEntity = userRoot -> {
            if (queryReference.get().getResultType().equals(User.class)) {
                return (Join<User, UserLogin>) userRoot.fetch(User_.logins);
            } else {
                return (Join<User, UserLogin>) userRoot.join(User_.logins);
            }
        };
        Function<Join<User, UserLogin>, Expression<String>> entityToColumn = entity -> entity.get(UserLogin_.login);
        return buildSpecification(loginFilter, functionToEntity.andThen(entityToColumn), queryConsumer);
    }

    private Specification<User> getLoginSpecificationForSoft(StringFilter loginFilter) {
        Consumer<CriteriaQuery<?>> queryConsumer = query -> query.distinct(true);

        Function<Root<User>, Join<User, UserLogin>> functionToEntity = userRoot -> (Join<User, UserLogin>) userRoot.join(User_.logins);
        Function<Join<User, UserLogin>, Expression<String>> entityToColumn = entity -> entity.get(UserLogin_.login);
        return buildSpecification(loginFilter, functionToEntity.andThen(entityToColumn), queryConsumer);
    }

    protected Specification<User> buildSpecification(StringFilter filter,
                                                     Function<Root<User>, Expression<String>> metaclassFunction,
                                                     Consumer<CriteriaQuery<?>> queryConsumer) {

        if (filter.getEquals() != null) {
            return equalsSpecification(metaclassFunction, filter.getEquals(), queryConsumer);
        } else if (filter.getIn() != null) {
            return valueIn(metaclassFunction, filter.getIn(), queryConsumer);
        } else if (filter.getContains() != null) {
            return likeUpperSpecification(metaclassFunction, filter.getContains(), queryConsumer);
        } else if (filter.getDoesNotContain() != null) {
            return doesNotContainSpecification(metaclassFunction, filter.getDoesNotContain(), queryConsumer);
        } else if (filter.getNotEquals() != null) {
            return notEqualsSpecification(metaclassFunction, filter.getNotEquals(), queryConsumer);
        } else if (filter.getSpecified() != null) {
            return byFieldSpecified(metaclassFunction, filter.getSpecified(), queryConsumer);
        }
        return null;
    }

    protected <X> Specification<User> equalsSpecification(Function<Root<User>, Expression<X>> metaclassFunction, X value, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            return builder.equal(metaclassFunction.apply(root), value);
        };
    }

    protected <X> Specification<User> notEqualsSpecification(Function<Root<User>, Expression<X>> metaclassFunction, X value, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            return builder.not(builder.equal(metaclassFunction.apply(root), value));
        };
    }

    protected Specification<User> doesNotContainSpecification(Function<Root<User>, Expression<String>> metaclassFunction, String value, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            return builder.not(builder.like(builder.upper(metaclassFunction.apply(root)), wrapLikeQuery(value)));
        };
    }

    protected Specification<User> likeUpperSpecification(Function<Root<User>, Expression<String>> metaclassFunction, String value, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            return builder.like(builder.upper(metaclassFunction.apply(root)), wrapLikeQuery(value));
        };
    }

    protected <X> Specification<User> byFieldSpecified(Function<Root<User>, Expression<X>> metaclassFunction, boolean specified, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            return specified ? builder.isNotNull(metaclassFunction.apply(root)) : builder.isNull(metaclassFunction.apply(root));
        };
    }

    protected <X> Specification<User> valueIn(Function<Root<User>, Expression<X>> metaclassFunction, Collection<X> values, Consumer<CriteriaQuery<?>> queryConsumer) {
        return (root, query, builder) -> {
            queryConsumer.accept(query);
            CriteriaBuilder.In<X> predicate = builder.in(metaclassFunction.apply(root));
            for (X value : values) {
                predicate = predicate.value(value);
            }
            return predicate;
        };
    }

    private Stream<Optional<Specification<User>>> buildDataAttributes(List<DataAttributeCriteria> dataAttributes) {
        return dataAttributes.stream()
            .map(this::buildDataSpecification)
            .map(Optional::ofNullable);
    }

    protected Specification<User> buildDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        if (dataAttributeCriteria.getOperation() == EQUALS) {
            return equalsDataSpecification(dataAttributeCriteria);
        } else if (dataAttributeCriteria.getOperation() == CONTAINS) {
            return likeDataSpecification(dataAttributeCriteria);
        }
        return null;
    }

    protected Specification<User> equalsDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        return (root, query, cb) -> {
            Expression<?> dataExpression = buildDataExpression(dataAttributeCriteria, root, cb);
            Expression<?> expression = customExpression.toExpression(cb, findValueByType(dataAttributeCriteria));
            return cb.equal(dataExpression, expression);
        };
    }

    protected Specification<User> likeDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        return (root, query, cb) -> {
            Expression<?> stringExpression = buildDataExpression(dataAttributeCriteria, root, cb);
            return cb.like(cb.upper(stringExpression.as(String.class)), wrapLikeQuery(dataAttributeCriteria.getValue()));
        };
    }

    protected Expression<?> buildDataExpression(DataAttributeCriteria dataAttributeCriteria, Root<User> root, CriteriaBuilder builder) {
        String jsonPath = "'$." + dataAttributeCriteria.getPath() + "'";
        return customExpression.jsonQuery(builder, root, User_.DATA, jsonPath);
    }

    private Object findValueByType(DataAttributeCriteria dataAttributeCriteria) {
        if (NUMBER == dataAttributeCriteria.getType()) {
            return Double.valueOf(dataAttributeCriteria.getValue());
        } else if (BOOLEAN == dataAttributeCriteria.getType()) {
            return Boolean.valueOf(dataAttributeCriteria.getValue());
        }

        return dataAttributeCriteria.getValue();
    }

}

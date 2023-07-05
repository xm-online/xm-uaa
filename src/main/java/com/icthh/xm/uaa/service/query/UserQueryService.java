package com.icthh.xm.uaa.service.query;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLogin_;
import com.icthh.xm.uaa.domain.User_;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.query.filter.DataAttributeCriteria;
import com.icthh.xm.uaa.service.query.filter.SoftUserFilterQuery;
import com.icthh.xm.uaa.service.query.filter.StrictUserFilterQuery;
import io.github.jhipster.service.QueryService;
import io.github.jhipster.service.filter.StringFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
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

import static com.icthh.xm.commons.migration.db.jsonb.CustomOracle12cDialect.JSON_QUERY;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService extends QueryService<User> {

    private final UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    public Page<UserDTO> findAllUsersByStrictMatch(StrictUserFilterQuery filterQuery, Pageable pageable) {
        final Session session = (Session) entityManager.getDelegate();
        final SessionFactoryImpl sessionFactory = (SessionFactoryImpl) session.getSessionFactory();
        final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
        log.info("Dialect: {}", dialect);
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
        List<Optional<Specification<User>>> dataAttributes = buildDataAttributes(filterQuery.getDataAttributes());
        return Stream.concat(filters, dataAttributes.stream());
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

    private List<Optional<Specification<User>>> buildDataAttributes(List<DataAttributeCriteria> dataAttributes) {
        log.info("Specification buildDataAttributes {}", dataAttributes);
        List<Optional<Specification<User>>> specs = new ArrayList<>();
        for (DataAttributeCriteria dataAttributeCriteria : dataAttributes) {
            log.info("DataAttribute path {}", dataAttributeCriteria.getPath());
            log.info("DataAttribute value {}", dataAttributeCriteria.getValue());
            log.info("DataAttribute operation {}", dataAttributeCriteria.getOperation());

            Specification<User> spec = buildDataSpecification(dataAttributeCriteria);
            specs.add(Optional.of(spec));

            log.info("Specification: {}", spec);
        }
        return specs;
    }

    protected Specification<User> buildDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        if (dataAttributeCriteria.getOperation().equals("equals")) {
            return equalsDataSpecification(dataAttributeCriteria);
        } else if (dataAttributeCriteria.getOperation().equals("contains")) {
            return likeDataSpecification(dataAttributeCriteria);
        }
        return null;
    }

    protected Specification<User> equalsDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        return (root, query, cb) -> {
            Expression<String> stringExpression = buildDataExpression(dataAttributeCriteria, root, cb);
            LiteralExpression<String> literalExpression = new LiteralExpression<>((CriteriaBuilderImpl) cb, String.class, dataAttributeCriteria.getValue());
            return cb.equal(stringExpression, literalExpression);
        };
    }

    protected Specification<User> likeDataSpecification(DataAttributeCriteria dataAttributeCriteria) {
        return (root, query, cb) -> {
            Expression<String> stringExpression = buildDataExpression(dataAttributeCriteria, root, cb);
            return cb.like(cb.upper(stringExpression), wrapLikeQuery(dataAttributeCriteria.getValue()));
        };
    }

    protected Expression<String> buildDataExpression(DataAttributeCriteria dataAttributeCriteria, Root<User> root, CriteriaBuilder builder) {
        return builder.function(
            "JSON_FAKE",
            String.class,
            root.get(User_.DATA).as(String.class),
            builder.literal("'$." + dataAttributeCriteria.getPath() + "'"));
//            new HibernateInlineExpression(builder, "'$." + dataAttributeCriteria.getPath() + "'"));
    }
}

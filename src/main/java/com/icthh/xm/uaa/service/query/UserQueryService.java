package com.icthh.xm.uaa.service.query;

import static java.util.Optional.ofNullable;

import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserLogin;
import com.icthh.xm.uaa.domain.UserLogin_;
import com.icthh.xm.uaa.domain.User_;
import com.icthh.xm.uaa.repository.UserRepository;
import com.icthh.xm.uaa.service.dto.UserDTO;
import com.icthh.xm.uaa.service.query.filter.UserFilterQuery;
import io.github.jhipster.service.QueryService;
import io.github.jhipster.service.filter.StringFilter;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService extends QueryService<User> {

    private final UserRepository userRepository;

    public Page<UserDTO> findAllUsersByStrictMatch(UserFilterQuery filterQuery, Pageable pageable) {
        Specification<User> specification = createStrictSpecification(filterQuery);
        return userRepository.findAll(specification, pageable).map(UserDTO::new);
    }

    public Page<UserDTO> findAllUsersBySoftMatch(String query, Pageable pageable) {
        Specification<User> specification = createSoftSpecification(query);
        return userRepository.findAll(specification, pageable).map(UserDTO::new);
    }

    private Specification<User> createStrictSpecification(UserFilterQuery filterQuery) {
        return createSpecs(filterQuery)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Specification.where(null), Specification::and);
    }

    private Specification<User> createSoftSpecification(String query) {
        StringFilter filter = new StringFilter();
        filter.setContains(query);
        return Specification.where(
            getLoginSpecification(filter)
            .or(buildStringSpecification(filter, User_.lastName))
            .or(buildStringSpecification(filter, User_.firstName))
        );
    }

    private Stream<Optional<Specification<User>>> createSpecs(UserFilterQuery filterQuery) {
        return Stream.of(
            ofNullable(filterQuery.getLogin()).map(this::getLoginSpecification),
            ofNullable(filterQuery.getLastName()).map(ln -> buildStringSpecification(ln, User_.lastName)),
            ofNullable(filterQuery.getFirstName()).map(fn -> buildStringSpecification(fn, User_.firstName)),
            ofNullable(filterQuery.getRoleKey()).map(fn -> buildStringSpecification(fn, User_.roleKey))
        );
    }

    private Specification<User> getLoginSpecification(StringFilter loginFilter) {
        Function<Root<User>, Join<User, UserLogin>> functionToEntity = userRoot -> (Join<User, UserLogin>)userRoot.join(User_.logins);
        Function<Join<User, UserLogin>, Expression<String>> entityToColumn = entity -> entity.get(UserLogin_.login);
        return buildSpecification(loginFilter, functionToEntity.andThen(entityToColumn));
    }

}

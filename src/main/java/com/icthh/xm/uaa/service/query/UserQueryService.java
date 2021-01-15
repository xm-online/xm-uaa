package com.icthh.xm.uaa.service.query;

import static java.util.Objects.nonNull;
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
import java.util.function.Function;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
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

    public Page<UserDTO> findAllUsers(UserFilterQuery filterQuery, Pageable pageable) {
        Specification<User> specification = createSpecification(filterQuery);
        return userRepository.findAll(specification, pageable).map(UserDTO::new);
    }

    private Specification<User> createSpecification(UserFilterQuery filterQuery) {

        Specification<User> initialSpec = Specification.where(null);
        Specification<User> loginSpec = ofNullable(filterQuery.getLogin())
            .map(l -> getLoginSpecification(l))
            .orElse(null);
        Specification<User> lastNameSpec = ofNullable(filterQuery.getLastName())
            .map(ln -> buildStringSpecification(ln, User_.lastName))
            .orElse(null);
        Specification<User> firstNameSpec = ofNullable(filterQuery.getFirstName())
            .map(fn -> buildStringSpecification(fn, User_.firstName))
            .orElse(null);

        initialSpec = and(initialSpec, loginSpec);
        initialSpec = and(initialSpec, lastNameSpec);
        initialSpec = and(initialSpec, firstNameSpec);
        return initialSpec;
    }

    private Specification<User> getLoginSpecification(StringFilter loginFilter) {
        Function<Root<User>, Join<User, UserLogin>> functionToEntity = userRoot -> (Join<User, UserLogin>)userRoot.fetch(User_.logins);
        Function<Join<User, UserLogin>, Expression<String>> entityToColumn = entity -> entity.get(UserLogin_.login);
        return buildSpecification(loginFilter, functionToEntity.andThen(entityToColumn));
    }

    private Specification<User> and(Specification<User> initSpec, Specification<User> andSpec){
        return nonNull(andSpec) ? initSpec.and(andSpec) : initSpec;
    }
}

package com.icthh.xm.uaa.service.query;

import com.icthh.xm.uaa.domain.Client;
import com.icthh.xm.uaa.domain.Client_;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.service.dto.ClientDTO;
import com.icthh.xm.uaa.service.query.filter.StrictClientFilterQuery;
import io.github.jhipster.service.QueryService;
import io.github.jhipster.service.filter.StringFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.metamodel.SingularAttribute;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientQueryService extends QueryService<Client> {

    private final ClientRepository clientRepository;

    public Page<ClientDTO> findAllByStrictMatch(StrictClientFilterQuery filterQuery, Pageable pageable) {
        Specification<Client> specification = createStrictSpecification(filterQuery);
        return clientRepository.findAll(specification, pageable).map(ClientDTO::new);
    }

    private Specification<Client> createStrictSpecification(StrictClientFilterQuery filterQuery) {
        return createStrictSpecs(filterQuery)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(Specification.where(null), Specification::and);
    }

    private Stream<Optional<Specification<Client>>> createStrictSpecs(StrictClientFilterQuery filterQuery) {
        return Stream.of(
            ofNullable(filterQuery.getClientId()).map(fn -> buildStringSpecification(fn, Client_.clientId)),
            ofNullable(filterQuery.getRoleKey()).map(fn -> buildStringSpecification(fn, Client_.roleKey)),
            ofNullable(filterQuery.getDescription()).map(fn -> buildStringSpecification(fn, Client_.description)),
            ofNullable(filterQuery.getState()).map(fn -> buildEnumSpecification(fn, Client_.state)),
            ofNullable(filterQuery.getScopes()).map(fn -> buildListSpecification(fn, Client_.SCOPES))
        );
    }

    private <E> Specification<Client> buildEnumSpecification(StringFilter filter, SingularAttribute<Client, E> field) {
        return buildSpecification(filter, root -> root.get(field).as(String.class));
    }

    private Specification<Client> buildListSpecification(StringFilter filter, String field) {
        return buildSpecification(filter, root -> root.get(field).as(String.class));
    }
}

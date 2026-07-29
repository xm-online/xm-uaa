package com.icthh.xm.uaa.web.rest.error;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/**
 * Recognises a persistence constraint violation and names the error it should be reported as.
 *
 * <p>This is the extension point of {@link UaaErrorTranslator}: a deployment that adds tables of its own
 * (an EE extension, a tenant specific schema object) teaches the translator about their constraints by
 * publishing an implementation as a Spring bean, without this module having to know those constraints exist.
 * {@link ConstraintNameErrorResolver} covers the usual "match the constraint name" case.
 *
 * <p>Resolvers are consulted in {@link org.springframework.core.annotation.Order} order and the first one to
 * return a value wins. Returning {@link Optional#empty()} means "not mine" and passes the violation on;
 * anything left unrecognised falls back to a generic error rather than surfacing SQL internals.
 */
@FunctionalInterface
public interface DataIntegrityErrorResolver {

    Optional<ErrorDefinition> resolve(DataIntegrityViolationException exception);
}

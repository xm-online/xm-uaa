package com.icthh.xm.uaa.web.rest.error;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Locale.ROOT;

/**
 * Resolves violations by looking for a constraint name in the driver's message.
 *
 * <p>Register one per module that owns tables, e.g.
 * <pre>
 * &#64;Bean
 * public DataIntegrityErrorResolver subscriptionErrorResolver() {
 *     return new ConstraintNameErrorResolver(Map.of(
 *         "uk_subscription_name", new ErrorDefinition("error.subscription.already.exists",
 *                                                    "Subscription already exists")));
 * }
 * </pre>
 *
 * <p>Matching is case insensitive and order preserving, so a more specific constraint can be listed ahead of a
 * broader one when names overlap.
 */
public class ConstraintNameErrorResolver implements DataIntegrityErrorResolver {

    private final Map<String, ErrorDefinition> errorsByConstraintName;

    public ConstraintNameErrorResolver(Map<String, ErrorDefinition> errorsByConstraintName) {
        // LinkedHashMap, not Map.copyOf: an immutable map does not preserve iteration order
        Map<String, ErrorDefinition> normalized = new LinkedHashMap<>();
        errorsByConstraintName.forEach((constraint, error) -> normalized.put(constraint.toLowerCase(ROOT), error));
        this.errorsByConstraintName = Collections.unmodifiableMap(normalized);
    }

    @Override
    public Optional<ErrorDefinition> resolve(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause.getMessage() == null) {
            return Optional.empty();
        }

        String details = cause.getMessage().toLowerCase(ROOT);
        return errorsByConstraintName.entrySet().stream()
            .filter(entry -> details.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }
}

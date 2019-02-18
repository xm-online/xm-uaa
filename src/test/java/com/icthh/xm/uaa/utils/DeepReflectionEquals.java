package com.icthh.xm.uaa.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;

import java.io.Serializable;
import org.mockito.ArgumentMatcher;

/**
 *  Argument matcher for deep reflection compare objects.
 *  For example: verify(service).doWord(deepRefEq(expected));
 */
public class DeepReflectionEquals<T> extends ArgumentMatcher<T> implements Serializable {

    private final Object wanted;
    private final String[] excludeFields;

    public DeepReflectionEquals(Object wanted, String... excludeFields) {
        this.wanted = wanted;
        this.excludeFields = excludeFields;
    }

    @Override
    public boolean matches(Object actual) {
        assertThat(wanted).usingComparatorForFields((o1, o2) -> 0, excludeFields)
            .isEqualToComparingFieldByFieldRecursively(actual);
        return true;
    }

    public String toString() {
        return "deepRefEq(" + this.wanted + ")";
    }

    public static <T> T deepRefEq(T value, String... excludeFields) {
        DeepReflectionEquals<T> deepReflectionEquals = new DeepReflectionEquals<T>(value, excludeFields);
        return argThat(deepReflectionEquals);
    }

}

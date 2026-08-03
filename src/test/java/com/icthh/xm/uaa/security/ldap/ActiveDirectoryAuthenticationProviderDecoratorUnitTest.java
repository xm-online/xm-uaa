package com.icthh.xm.uaa.security.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.icthh.xm.uaa.domain.properties.TenantProperties;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.security.authentication.AuthenticationProvider;

@RunWith(MockitoJUnitRunner.class)
public class ActiveDirectoryAuthenticationProviderDecoratorUnitTest {

    private static final String LOGIN = "Homer";
    private static final String AUTH_FIELD = "userPrincipalName";
    private static final String USER_PRINCIPAL_NAME = "homer@company.com";
    private static final String ROOT_DN = "dc=company,dc=com";

    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private TenantProperties.Ldap ldap;

    @Mock
    private DirContext ctx;

    private ActiveDirectoryAuthenticationProviderDecorator decorator;

    @Before
    public void setUp() throws NamingException {
        when(ldap.getRootDn()).thenReturn(ROOT_DN);
        when(ldap.getSearchFields()).thenReturn("(sAMAccountName={0})");
        when(ctx.getNameInNamespace()).thenReturn(ROOT_DN);
        decorator = new ActiveDirectoryAuthenticationProviderDecorator(authenticationProvider, ldap) {
            @Override
            protected DirContext bindBySystemUser() {
                return ctx;
            }
        };
    }

    @Test
    public void shouldCloseContextAfterSuccessfulSearch() throws NamingException {
        when(ldap.getAuthField()).thenReturn(AUTH_FIELD);
        DirContextAdapter foundUser = new DirContextAdapter();
        foundUser.setAttributeValue(AUTH_FIELD, USER_PRINCIPAL_NAME);
        mockSearchResult(new SearchResult("cn=homer", foundUser, new BasicAttributes()));

        String userPrincipalName = decorator.findUserPrincipalName(LOGIN);

        assertEquals(USER_PRINCIPAL_NAME, userPrincipalName);
        verify(ctx).close();
    }

    @Test
    public void shouldCloseContextWhenUserNotFound() throws NamingException {
        mockEmptySearchResult();

        try {
            decorator.findUserPrincipalName(LOGIN);
            fail("Expected IncorrectResultSizeDataAccessException");
        } catch (IncorrectResultSizeDataAccessException expected) {
            // user not found
        }

        verify(ctx).close();
    }

    @SuppressWarnings("unchecked")
    private void mockSearchResult(SearchResult searchResult) throws NamingException {
        NamingEnumeration<SearchResult> resultsEnum = mock(NamingEnumeration.class);
        when(resultsEnum.hasMore()).thenReturn(true, false);
        when(resultsEnum.next()).thenReturn(searchResult);
        when(ctx.search(any(Name.class), anyString(), any(Object[].class), any(SearchControls.class)))
            .thenReturn(resultsEnum);
    }

    @SuppressWarnings("unchecked")
    private void mockEmptySearchResult() throws NamingException {
        NamingEnumeration<SearchResult> resultsEnum = mock(NamingEnumeration.class);
        when(resultsEnum.hasMore()).thenReturn(false);
        when(ctx.search(any(Name.class), anyString(), any(Object[].class), any(SearchControls.class)))
            .thenReturn(resultsEnum);
    }
}

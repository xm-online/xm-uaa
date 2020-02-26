package com.icthh.xm.uaa.lep;

import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.lep.api.LepManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static com.icthh.xm.commons.lep.XmLepConstants.THREAD_CONTEXT_KEY_TENANT_CONTEXT;
import static com.icthh.xm.commons.lep.XmLepScriptConstants.BINDING_KEY_AUTH_CONTEXT;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Component
@RequiredArgsConstructor
@Order(HIGHEST_PRECEDENCE)
public class LepThreadContextFilter implements Filter {

    private final LepManager lepManager;
    private final TenantContextHolder tenantContextHolder;
    private final XmAuthenticationContextHolder xmAuthContextHolder;

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

        lepManager.beginThreadContext(scopedContext -> {
            scopedContext.setValue(THREAD_CONTEXT_KEY_TENANT_CONTEXT, tenantContextHolder.getContext());
            scopedContext.setValue(BINDING_KEY_AUTH_CONTEXT, xmAuthContextHolder.getContext());
        });

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        lepManager.endThreadContext();
    }
}

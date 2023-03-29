package com.icthh.xm.uaa.service.mail;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.uaa.UaaApp;
import com.icthh.xm.uaa.config.xm.XmOverrideConfiguration;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        UaaApp.class,
        XmOverrideConfiguration.class
})
public class MailServiceTemplateIntTest {

    private static final String TEMPLATE_NAME = "templateName";
    private static final String SUBJECT = "subject";
    private static final String EMAIL = "email";
    public static final String TENANT_NAME = "RESINTTEST";

    @SpyBean
    private MailService mailService;

    @Autowired
    private TenantEmailTemplateService templateService;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private TenantPropertiesService tenantPropertiesService;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Mock
    private XmAuthenticationContextHolder authContextHolder;

    @Mock
    private XmAuthenticationContext context;

    @SneakyThrows
    @Before
    public void setup() {
        TenantContextUtils.setTenant(tenantContextHolder, TENANT_NAME);
        MockitoAnnotations.initMocks(this);
        when(authContextHolder.getContext()).thenReturn(context);
        when(context.getUserKey()).thenReturn(Optional.of("userKey"));
    }

    @Test
    public void testComplexTemplateEmail() {

        String mainPath = "/config/tenants/" + TENANT_NAME + "/uaa/emails/en/" + TEMPLATE_NAME + ".ftl";
        String basePath = "/config/tenants/" + TENANT_NAME + "/uaa/emails/en/" + TEMPLATE_NAME + "-BASE.ftl";
        String body = "<#import \"/" + TENANT_NAME + "/en/" + TEMPLATE_NAME + "-BASE\" as main>OTHER_<@main.body>_CUSTOM_</@main.body>";
        String base = "<#macro body>BASE_START<#nested>BASE_END</#macro>";
        templateService.onRefresh(mainPath, body);
        templateService.onRefresh(basePath, base);
        mailService.sendEmailFromTemplate(TenantKey.valueOf(TENANT_NAME), new User(), TEMPLATE_NAME, SUBJECT, EMAIL, Map.of(
                "variable1", "value1",
                "variable2", "value2"
        ));

        verify(mailService).sendEmail(any(), any(), eq("OTHER_BASE_START_CUSTOM_BASE_END"), any(), any());
    }

}

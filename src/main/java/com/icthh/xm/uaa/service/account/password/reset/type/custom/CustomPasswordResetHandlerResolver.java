package com.icthh.xm.uaa.service.account.password.reset.type.custom;

import static org.apache.commons.lang3.StringUtils.upperCase;

import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.uaa.service.account.password.reset.PasswordResetRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CustomPasswordResetHandlerResolver implements LepKeyResolver {

    public static final String RESET_REQUEST = "resetRequest";

    @Override
    public List<String> segments(LepMethod method) {
        PasswordResetRequest resetType = method.getParameter(RESET_REQUEST, PasswordResetRequest.class);
        return List.of(
            upperCase(resetType.getResetType())
        );
    }
}

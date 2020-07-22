package com.icthh.xm.uaa.service.account.password.reset;

import com.icthh.xm.uaa.service.account.password.reset.type.custom.CustomPasswordResetHandler;
import com.icthh.xm.uaa.service.account.password.reset.type.email.EmailPasswordResetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
public class PasswordResetHandlerFactory {
    public static final String EMAIL_TYPE = "EMAIL";

    private final CustomPasswordResetHandler customPasswordResetHandler;
    private final EmailPasswordResetHandler emailPasswordResetHandler;

    private Map<String, PasswordResetHandler> registeredHandlers;

    @PostConstruct
    private void init() {
        this.registeredHandlers = new HashMap<>();
        this.registeredHandlers.put(EMAIL_TYPE, emailPasswordResetHandler);
    }

    public PasswordResetHandler getPasswordResetHandler(String resetType) {
        ofNullable(resetType).orElseThrow(() -> new IllegalArgumentException("Reset type must exist"));
        return ofNullable(registeredHandlers.get(resetType)).orElse(customPasswordResetHandler);
    }

}

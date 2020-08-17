package com.icthh.xm.uaa.service.account.password.reset;

import com.icthh.xm.uaa.service.account.password.reset.type.custom.CustomPasswordResetHandler;
import com.icthh.xm.uaa.service.account.password.reset.type.email.EmailPasswordResetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
        resetType = Objects.requireNonNull(resetType, "resetType can't be null");
        return registeredHandlers.getOrDefault(resetType, customPasswordResetHandler);
    }

}

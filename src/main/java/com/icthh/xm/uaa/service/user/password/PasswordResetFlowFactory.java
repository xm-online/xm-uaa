package com.icthh.xm.uaa.service.user.password;

import com.icthh.xm.uaa.service.user.password.reset.email.EmailPasswordResetHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Component
@RequiredArgsConstructor
public class PasswordResetFlowFactory {

    private final CustomPasswordResetHandler customPasswordResetHandler;
    private final EmailPasswordResetHandler emailPasswordResetHandler;

    private Map<String, PasswordResetHandler> registeredHandlers;

    @PostConstruct
    private void init() {
        this.registeredHandlers = new HashMap<>();
        this.registeredHandlers.put("EMAIL", emailPasswordResetHandler);
    }

    public PasswordResetHandler getPasswordResetHandler(String resetType) {
        ofNullable(resetType).orElseThrow(() -> new IllegalArgumentException("Reset type must exists"));
        return ofNullable(registeredHandlers.get(resetType)).orElse(customPasswordResetHandler);
    }

}

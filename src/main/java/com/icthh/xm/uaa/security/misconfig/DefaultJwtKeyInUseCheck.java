package com.icthh.xm.uaa.security.misconfig;

import com.icthh.xm.uaa.config.UaaKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class DefaultJwtKeyInUseCheck implements MisconfigurationCheck {

    private static final String MESSAGE =
        "!!! MISCONFIGURATION !!! CRITICAL !!! Using DEFAULT keystore." +
            " Provide a unique signing key via application.private-key ";

    private final UaaKeyProvider keyProvider;

    @Override
    public Result check() {
        return keyProvider.isDefaultKeystoreInUse() ? Result.misconfigured(MESSAGE) : Result.ok();
    }
}

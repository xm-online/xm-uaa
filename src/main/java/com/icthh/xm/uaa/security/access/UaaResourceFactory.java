package com.icthh.xm.uaa.security.access;

import com.icthh.xm.commons.permission.access.ResourceFactory;
import com.icthh.xm.commons.permission.access.repository.ResourceRepository;
import com.icthh.xm.uaa.repository.ClientRepository;
import com.icthh.xm.uaa.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class UaaResourceFactory implements ResourceFactory {
    private Map<String, ResourceRepository> repositories = new HashMap<>();

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public UaaResourceFactory(UserRepository userRepository, ClientRepository clientRepository) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @PostConstruct
    public void init() {
        repositories.put("client", clientRepository);
        repositories.put("user", userRepository);
    }

    @Override
    public Object getResource(Object resourceId, String objectType) {
        Object result = null;
        ResourceRepository resourceRepository = repositories.get(objectType);
        if (resourceRepository != null) {
            result = resourceRepository.findResourceById(resourceId);
        }
        return result;
    }
}

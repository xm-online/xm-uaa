package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.uaa.service.LdapService;
import io.github.jhipster.web.util.ResponseUtil;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class LdapResource {

    private final LdapService ldapService;

    @GetMapping("/ldap/_search-with-template")
    @Timed
    public ResponseEntity searchByTemplate(@NotEmpty @RequestParam String ldapDomain,
                                           @NotEmpty @RequestParam String templateKey) {
        Set<Map<String, List<String>>> result = ldapService.searchByTemplate(ldapDomain, templateKey);
        return ResponseUtil.wrapOrNotFound(Optional.of(result));
    }
}

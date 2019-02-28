package com.icthh.xm.uaa.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.uaa.domain.properties.TenantLogins;
import com.icthh.xm.uaa.service.TenantLoginsService;
import com.icthh.xm.uaa.web.rest.vm.UaaValidationVM;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

@Api(value = "uaa")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantLoginsResource {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final TenantLoginsService tenantLoginsService;

    /**
     * Get login properties.
     * @return login properties
     */
    @GetMapping(value = "/logins")
    @ApiOperation(value = "Get uaa login properties", response = ResponseEntity.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Uaa properties update result", response = ResponseEntity.class),
        @ApiResponse(code = 500, message = "Internal server error")})
    @Timed
    @PostAuthorize("hasPermission({'returnObject': returnObject.body}, 'TENANT.LOGIN.GET_LIST')")
    public ResponseEntity<TenantLogins> getLogins() {
        return ResponseEntity.ok(tenantLoginsService.getLogins());
    }

    /**
     * Validate logins yml.
     * @param loginsYml logins yml
     * @return true if valid
     */
    @PostMapping(value = "/logins/validate", consumes = {TEXT_PLAIN_VALUE})
    @ApiOperation(value = "Validate uaa login properties format", response = UaaValidationVM.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Uaa validation result", response = UaaValidationVM.class),
        @ApiResponse(code = 500, message = "Internal server error")})
    @SneakyThrows
    @Timed
    @PreAuthorize("hasPermission(null, 'TENANT.LOGIN.VALIDATE')")
    public UaaValidationVM validate(@RequestBody String loginsYml) {
        try {
            mapper.readValue(loginsYml, TenantLogins.class);
            return UaaValidationVM.builder().isValid(true).build();
        } catch (JsonParseException | JsonMappingException e) {
            return UaaValidationVM.builder().isValid(false).errorMessage(e.getLocalizedMessage()).build();
        }
    }

    /**
     * Update logins yml.
     * @param loginsYml new logins yml
     * @return void
     */
    @PostMapping(value = "/logins", consumes = {TEXT_PLAIN_VALUE})
    @ApiOperation(value = "Update uaa login properties", response = ResponseEntity.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Uaa login properties update result", response = ResponseEntity.class),
        @ApiResponse(code = 500, message = "Internal server error")})
    @SneakyThrows
    @Timed
    @PreAuthorize("hasPermission({'loginsYml': #loginsYml}, 'TENANT.LOGIN.UPDATE')")
    public ResponseEntity<Void> updateLogins(@RequestBody String loginsYml) {
        tenantLoginsService.updateLogins(loginsYml);
        return ResponseEntity.ok().build();
    }

}

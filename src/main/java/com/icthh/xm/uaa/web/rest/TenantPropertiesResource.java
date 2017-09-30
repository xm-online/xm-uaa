package com.icthh.xm.uaa.web.rest;

import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.uaa.domain.properties.TenantProperties;
import com.icthh.xm.uaa.service.TenantPropertiesService;
import com.icthh.xm.uaa.web.rest.vm.UaaValidationVM;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "uaa")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TenantPropertiesResource {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private final TenantPropertiesService tenantPropertiesService;

    @PostMapping(value = "/uaa/properties/validate", consumes = {TEXT_PLAIN_VALUE})
    @ApiOperation(value = "Validate uaa properties format", response = UaaValidationVM.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Uaa validation result", response = UaaValidationVM.class),
        @ApiResponse(code = 500, message = "Internal server error")})
    @SneakyThrows
    @Timed
    public UaaValidationVM validate(@RequestBody String timelineYml) {
        try {
            mapper.readValue(timelineYml, TenantProperties.class);
            return UaaValidationVM.builder().isValid(true).build();
        } catch (JsonParseException | JsonMappingException e) {
            return UaaValidationVM.builder().isValid(false).errorMessage(e.getLocalizedMessage()).build();
        }
    }

    @PostMapping(value = "/uaa/properties", consumes = {TEXT_PLAIN_VALUE})
    @ApiOperation(value = "Update uaa properties", response = ResponseEntity.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Uaa properties update result", response = ResponseEntity.class),
        @ApiResponse(code = 500, message = "Internal server error")})
    @SneakyThrows
    @Timed
    public ResponseEntity<Void> updateTenantProperties(@RequestBody String timelineYml) {
        tenantPropertiesService.updateTenantProps(timelineYml);
        return ResponseEntity.ok().build();
    }

}

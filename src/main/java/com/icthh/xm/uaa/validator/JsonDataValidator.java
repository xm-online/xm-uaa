package com.icthh.xm.uaa.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.UserSpec;
import com.icthh.xm.uaa.service.UserSpecService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.MapUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.fge.jsonschema.core.report.LogLevel.ERROR;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

@Slf4j
@RequiredArgsConstructor
public class JsonDataValidator implements ConstraintValidator<com.icthh.xm.uaa.validator.JsonData, User> {

    private final String REGEX_EOL = "\n";

    private final ObjectMapper objectMapper;
    private final UserSpecService userSpecService;

    @Override
    public void initialize(com.icthh.xm.uaa.validator.JsonData constraintAnnotation) {
        log.trace("Json data validator inited");
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public boolean isValid(User user, ConstraintValidatorContext context) {
        List<UserSpec> userSpec = userSpecService.getUserSpec(user.getAuthorities());

        if (userSpec.isEmpty()) {
            // if user specification is not found we return successful validation result
            // to support backward compatibility case
            return true;
        }

        if (MapUtils.isEmpty(user.getData())) {
            log.error("User specification is not null, but data is null: {}", user.getData());
            return false;
        }
        return  userSpec.stream().allMatch(spec -> validate(user.getData(), spec.getDataSpec(), context));
    }

    @SneakyThrows
    private boolean validate(Map<String, Object> data, String jsonSchema, ConstraintValidatorContext context) {
        String stringData = objectMapper.writeValueAsString(data);
        log.debug("Validation data. map: {}, jsonData: {}", data, stringData);

        JsonNode schemaNode = JsonLoader.fromString(jsonSchema);

        JsonNode dataNode = JsonLoader.fromString(stringData);
        JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        JsonSchema schema = factory.getJsonSchema(schemaNode);
        val report = schema.validate(dataNode);

        boolean isSuccess = report.isSuccess();
        if (!isSuccess) {
            log.error("Validation data report: {}", report.toString().replaceAll(REGEX_EOL, " | "));
            context.disableDefaultConstraintViolation();

            List<?> message = stream(report.spliterator(), false)
                .filter(error -> error.getLogLevel().equals(ERROR)).map(ProcessingMessage::asJson).collect(toList());
            context.buildConstraintViolationWithTemplate(objectMapper.writeValueAsString(message))
                .addConstraintViolation();
        }
        return isSuccess;
    }
}

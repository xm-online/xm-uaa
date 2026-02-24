package com.icthh.xm.uaa.config.timezone;

import com.icthh.xm.uaa.IntegrationTest;
import com.icthh.xm.uaa.domain.DateTimeWrapper;
import com.icthh.xm.uaa.repository.DateTimeWrapperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for verifying the behavior of Hibernate in the context of storing various date and time types
 * across different databases. The tests focus on ensuring that the stored values are correctly transformed and
 * stored according to the configured timezone.
 * Timezone is environment specific, and can be adjusted according to your needs.
 * <p>
 * For more context, refer to:
 * - GitHub Issue: <a href="https://github.com/jhipster/generator-jhipster/issues/22579">...</a>
 <a href="*">- Pull Request: https://github.com/jhipster/generator-j</a>hipster/pull/22946
 */
@IntegrationTest
class HibernateTimeZoneIntTest {

    public static final int BASE_EPOCH_YEAR = 1970;

    @Autowired
    private DateTimeWrapperRepository dateTimeWrapperRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${spring.jpa.properties.hibernate.jdbc.time_zone:UTC}")
    private String zoneId;

    private DateTimeWrapper dateTimeWrapper;
    private DateTimeFormatter dateTimeFormatter;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter offsetTimeFormatter;
    private DateTimeFormatter dateFormatter;

    @BeforeEach
    void setup() {
        dateTimeWrapper = new DateTimeWrapper();
        dateTimeWrapper.setInstant(Instant.parse("2014-11-12T05:10:00.0Z"));
        dateTimeWrapper.setLocalDateTime(LocalDateTime.parse("2014-11-12T07:20:00.0"));
        dateTimeWrapper.setOffsetDateTime(OffsetDateTime.parse("2011-12-14T08:30:00.0Z"));
        dateTimeWrapper.setZonedDateTime(ZonedDateTime.parse("2011-12-14T08:40:00.0Z"));
        dateTimeWrapper.setLocalTime(LocalTime.parse("14:50:00"));
        dateTimeWrapper.setOffsetTime(OffsetTime.parse("14:00:00+02:00"));
        dateTimeWrapper.setLocalDate(LocalDate.parse("2016-09-10"));

        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S").withZone(ZoneId.of(zoneId));
        timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of(zoneId));
        offsetTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Test
    @Transactional
    void storeInstantWithZoneIdConfigShouldBeStoredOnConfiguredTimeZone() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("instant", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeFormatter.format(dateTimeWrapper.getInstant());

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeLocalDateTimeWithZoneIdConfigShouldBeStoredOnConfiguredTimeZone() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("local_date_time", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper.getLocalDateTime().format(dateTimeFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeOffsetDateTimeWithZoneIdConfigShouldBeStoredOnConfiguredTimeZone() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("offset_date_time", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper.getOffsetDateTime().format(dateTimeFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeZoneDateTimeWithZoneIdConfigShouldBeStoredOnConfiguredTimeZone() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("zoned_date_time", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper.getZonedDateTime().format(dateTimeFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeLocalTimeWithZoneIdConfigShouldBeStoredOnConfiguredTimeZoneAccordingToHis1stJan1970Value() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("local_time", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper
            .getLocalTime()
            .atDate(LocalDate.of(BASE_EPOCH_YEAR, Month.JANUARY, 1))
            .format(timeFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeOffsetTimeWithZoneIdConfigShouldBeStoredOnConfiguredTimeZoneAccordingToHis1stJan1970Value() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("offset_time", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper
            .getOffsetTime()
            // Convert to configured timezone
            .withOffsetSameInstant(ZoneId.of(zoneId).getRules().getOffset(Instant.now()))
            // Normalize to System TimeZone.
            // this behavior looks a bug, refer to https://github.com/jhipster/generator-jhipster/issues/22579.
            .withOffsetSameLocal(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()).getOffset())
            // Convert the normalized value to configured timezone
            .withOffsetSameInstant(ZoneId.of(zoneId).getRules().getOffset(Instant.EPOCH))
            .format(offsetTimeFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    @Test
    @Transactional
    void storeLocalDateWithZoneIdConfigShouldBeStoredWithoutTransformation() {
        dateTimeWrapperRepository.saveAndFlush(dateTimeWrapper);

        String request = generateSqlRequest("local_date", dateTimeWrapper.getId());
        SqlRowSet resultSet = jdbcTemplate.queryForRowSet(request);
        String expectedValue = dateTimeWrapper.getLocalDate().format(dateFormatter);

        assertThatValueFromSqlRowSetIsEqualToExpectedValue(resultSet, expectedValue);
    }

    private String generateSqlRequest(String fieldName, long id) {
        return format("SELECT %s FROM jhi_date_time_wrapper where id=%d", fieldName, id);
    }

    private void assertThatValueFromSqlRowSetIsEqualToExpectedValue(SqlRowSet sqlRowSet, String expectedValue) {
        while (sqlRowSet.next()) {
            String dbValue = sqlRowSet.getString(1);

            assertThat(dbValue).isNotNull();
            assertThat(dbValue).isEqualTo(expectedValue);
        }
    }
}

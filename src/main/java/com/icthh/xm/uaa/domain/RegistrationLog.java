package com.icthh.xm.uaa.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "registration_log")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationLog {

    @Id
    @NotNull
    @Column(name = "ip_address", unique = true, nullable = false)
    private String ipAddress;

    @Column(name = "last_registration_time")
    @JsonIgnore
    private Instant lastRegistrationTime = Instant.now();

    public RegistrationLog(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @PrePersist @PreUpdate
    private void preSave() {
        lastRegistrationTime = Instant.now();
    }

    public boolean moreThenSecondsAgo(long seconds) {
        return lastRegistrationTime.plusSeconds(seconds).isAfter(Instant.now());
    }

}

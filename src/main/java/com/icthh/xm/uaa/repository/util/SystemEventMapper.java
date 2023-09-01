package com.icthh.xm.uaa.repository.util;

import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_IMAGE_URL;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE;
import static com.icthh.xm.uaa.config.Constants.SYSTEM_EVENT_PROP_ROLE_KEY;

import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.uaa.domain.User;

import java.time.Instant;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class SystemEventMapper {

    /**
     * Mapping system event to user.
     *
     * @param event the system event.
     * @param user  the user.
     */
    public static void toUser(SystemEvent event, User user) {
        Map<String, Object> data = event.getDataMap();

        user.setImageUrl(String.valueOf(data.get(SYSTEM_EVENT_PROP_IMAGE_URL)));

        String updateDateStr = String.valueOf(data.get(SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE));
        Instant updateDate = StringUtils.isNotBlank(updateDateStr) ? Instant.parse(updateDateStr) : Instant.now();

        Object roleKey = data.get(SYSTEM_EVENT_PROP_ROLE_KEY);
        if (roleKey != null && StringUtils.isNotBlank(roleKey.toString())) {
            user.setRoleKey(roleKey.toString());
        }

        user.setLastModifiedDate(updateDate);

        user.setLastModifiedBy(event.getUserLogin());
    }

}

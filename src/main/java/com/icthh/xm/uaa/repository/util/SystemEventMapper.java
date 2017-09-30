package com.icthh.xm.uaa.repository.util;

import com.icthh.xm.uaa.config.Constants;
import com.icthh.xm.uaa.domain.User;
import com.icthh.xm.uaa.domain.kafka.SystemEvent;
import java.time.Instant;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class SystemEventMapper {

    /**
     * Mapping system event to user.
     * @param event the system event.
     * @param user the user.
     */
    public static void toUser(SystemEvent event, User user) {
        Map<String, Object> data = event.getData();

        user.setFirstName("");
        user.setLastName("");
        String name = String.valueOf(data.get(Constants.NAME));
        if (StringUtils.isNotBlank(name)) {
            String[] nameArr = org.apache.commons.lang3.StringUtils.split(name, " ", 2);

            user.setFirstName(nameArr[0]);

            if (nameArr.length == 2) {
                user.setLastName(nameArr[1]);
            }
        }

        user.setImageUrl(String.valueOf(data.get(Constants.IMAGE_URL)));

        String updateDate = String.valueOf(data.get(Constants.LAST_MODIFIED_DATE));
        if (StringUtils.isNotBlank(updateDate)) {
            user.setLastModifiedDate(Instant.parse(updateDate));
        } else {
            user.setLastModifiedDate(Instant.now());
        }
        user.setLastModifiedBy(event.getTenantInfo().getUserLogin());

    }
}

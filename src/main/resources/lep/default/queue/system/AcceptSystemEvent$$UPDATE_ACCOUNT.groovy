import com.icthh.xm.commons.messaging.event.system.SystemEvent
import com.icthh.xm.uaa.config.Constants
import com.icthh.xm.uaa.domain.User
import com.icthh.xm.uaa.lep.LepContext
import com.icthh.xm.uaa.repository.util.SystemEventMapper
import com.icthh.xm.uaa.service.UserService
import org.slf4j.LoggerFactory

log = LoggerFactory.getLogger(getClass())
LepContext context = lepContext
onUpdateAccount(context)

private void onUpdateAccount(LepContext context) {

    SystemEvent event = context.inArgs.event
    String userKey = event.getDataMap().get(Constants.SYSTEM_EVENT_PROP_USER_KEY)
    UserService userService = context.services.userService

    log.info("Start to update account for userKey='{}'", userKey)
    User user =  userService.getUser(userKey)
    if (user == null) {
        log.error("Failed to update account. User with userKey='{}' does not exists.", userKey)
    } else {
        SystemEventMapper.toUser(event, user)
        userService.saveUser(user)
    }
}

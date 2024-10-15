package com.icthh.xm.uaa.service.messaging;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.lep.LogicExtensionPoint;
import com.icthh.xm.commons.lep.spring.LepService;
import com.icthh.xm.uaa.service.dto.OtpSendDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@LepService(group = "service.user")
public class UserMessagingService {

    @LogicExtensionPoint(value = "SendAuthorizeMessage")
    public void sendAuthorizeMessage(OtpSendDTO otpSendDTO) {
        throw new BusinessException("Send authorize message logic is not implemented!");
    }
}

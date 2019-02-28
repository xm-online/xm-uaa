package com.icthh.xm.uaa.social.exceptions;

import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.uaa.domain.User;

import java.util.List;

public class FoundMoreThanOneUserBySocialUserInfo extends BusinessException {
    public FoundMoreThanOneUserBySocialUserInfo(List<User> existingUser) {
        super("error.found.more.than.one.user.by.social.login", "Found more than one users by social info.");
    }
}

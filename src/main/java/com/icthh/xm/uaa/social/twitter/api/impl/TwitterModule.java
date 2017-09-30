package com.icthh.xm.uaa.social.twitter.api.impl;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.icthh.xm.uaa.social.twitter.api.TwitterProfile;

/**
 * Jackson module for registering mixin annotations against Twitter model classes.
 */
@SuppressWarnings("serial")
class TwitterModule extends SimpleModule {

    public TwitterModule() {
        super("TwitterModule");
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(TwitterProfile.class, TwitterProfileMixin.class);
    }

}

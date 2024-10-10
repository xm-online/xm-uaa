package com.icthh.xm.uaa.config;

/**
 * Application constants.
 */
public final class Constants {

    public static final String REQUEST_CTX_PROTOCOL = "protocol";
    public static final String REQUEST_CTX_DOMAIN = "domain";
    public static final String REQUEST_CTX_PORT = "port";
    public static final String REQUEST_CTX_WEB_APP = "webapp";

    //Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";
    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";

    public static final String TENANT_REGEX = "^[A-Za-z_]+$";
    public static final String HEADER_SCHEME = "x-scheme";
    public static final String HEADER_DOMAIN = "x-domain";
    public static final String HEADER_PORT = "x-port";
    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";
    public static final String AUTH_PROVIDER_KEY = "provider";
    public static final String AUTH_TENANT_KEY = "tenant";
    public static final String AUTH_USER_KEY = "user_key";
    public static final String AUTH_USER_NAME = "user_name";
    public static final String AUTH_USERNAME = "username";
    public static final String AUTH_USERNAME_DOMAIN_SEPARATOR = "@";
    public static final String AUTH_ROLE_KEY = "role_key";
    public static final String AUTH_LOGINS_KEY = "logins";
    public static final String KEYSTORE_PATH = "keystore.jks";
    public static final String KEYSTORE_PSWRD = "password";
    public static final String KEYSTORE_ALIAS = "selfsigned";
    public static final String DDL_CREATE_SCHEMA = "CREATE SCHEMA IF NOT EXISTS %s";
    public static final String DB_SCHEMA_CREATETION_ENABLED = "db.schema.creation.enabled";
    public static final String CHANGE_LOG_PATH = "classpath:config/liquibase/master.xml";
    public static final String LOGIN_USED_CODE = "error.login.already.used";
    public static final String LOGIN_NOT_PROVIDED_CODE = "error.login.not.provided";
    public static final String LOGIN_USED_PARAM = "loginTypeKey";
    public static final String LOGIN_IS_USED_ERROR_TEXT = "Login already in use";
    public static final String LOGIN_NOT_PROVIDED_ERROR_TEXT = "Login is not provided";
    public static final String ACTIVATE_PROFILE_EVENT_TYPE = "ACTIVATE_PROFILE";
    public static final String CREATE_PROFILE_EVENT_TYPE = "CREATE_PROFILE";
    public static final String CHANGE_PASSWORD_EVENT_TYPE = "CHANGE_PASSWORD";
    public static final String UPDATE_PROFILE_EVENT_TYPE = "UPDATE_PROFILE";
    public static final String UPDATE_ACCOUNT_EVENT_TYPE = "UPDATE_ACCOUNT";
    public static final String DELETE_PROFILE_EVENT_TYPE = "DELETE_PROFILE";
    public static final String AUTH_ADDITIONAL_DETAILS = "additionalDetails";

    public static final String CREATE_TOKEN_TIME = "createTokenTime";
    public static final String MULTI_ROLE_ENABLED = "multiRoleEnabled";
    public static final String TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY = "tfaVerificationKey";
    public static final String TOKEN_AUTH_DETAILS_TFA_OTP_CHANNEL_TYPE = "tfaOtpChannel";
    public static final String TOKEN_AUTH_DETAILS_TFA_OTP_ID = "otpId";
    public static final String TOKEN_AUTH_DETAILS_TFA_DESTINATION = "destination";

    public static final String ACCESS_TOKEN_URL = "http://uaa/oauth/token";

    public static final String REQ_ATTR_TFA_VERIFICATION_OTP_KEY = TOKEN_AUTH_DETAILS_TFA_VERIFICATION_OTP_KEY;
    public static final String REQ_ATTR_TFA_OTP_CHANNEL_TYPE = TOKEN_AUTH_DETAILS_TFA_OTP_CHANNEL_TYPE;

    public static final String HTTP_HEADER_TFA_OTP = "Icthh-Xm-Tfa-Otp";
    public static final String HTTP_HEADER_TFA_OTP_CHANNEL = "Icthh-Xm-Tfa-Otp-Channel";

    public static final String CERTIFICATE = "X.509";
    public static final String KEYSTORE_TYPE = "PKCS12";

    // System event data properties
    public static final String SYSTEM_EVENT_PROP_ID = "id";
    public static final String SYSTEM_EVENT_PROP_NAME = "name";
    public static final String SYSTEM_EVENT_PROP_IMAGE_URL = "imageUrl";
    public static final String SYSTEM_EVENT_PROP_CREATED_DATE = "createdDate";
    public static final String SYSTEM_EVENT_PROP_LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String SYSTEM_EVENT_PROP_USER_KEY = "userKey";
    public static final String SYSTEM_EVENT_PROP_ROLE_KEY = "roleKey";

    public static final String DEFAULT_CONFIG_PATH = "config/specs/default-uaa.yml";
    public static final String DEFAULT_LOGINS_CONFIG_PATH = "config/specs/default-logins.yml";

    public static final String PATH_TO_EMAILS = "/config/emails/";

    public static final String DEFAULT_EMAILS_PATTERN = "classpath*:config/emails/**/*.ftl";
    public static final String DEFAULT_EMAILS_PATH_PATTERN = "/**/config/emails/{lang}/{name}.ftl";

    public static final String PATH_TO_EMAILS_IN_CONFIG = "/emails/";

    public static final String WEB_APP_CLIENT = "web_app";
    public static final String SUPER_TENANT = "XM";

    public static final String TRANSLATION_KEY = "trKey";

    private Constants() {
    }

}

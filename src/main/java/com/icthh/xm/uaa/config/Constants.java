package com.icthh.xm.uaa.config;

/**
 * Application constants.
 */
public final class Constants {

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
    public static final String KEYSTORE_PATH = "keystore.jks";
    public static final String KEYSTORE_PSWRD = "password";
    public static final String KEYSTORE_ALIAS = "selfsigned";
    public static final String DEFAULT_TENANT = "XM";
    public static final String DDL_CREATE_SCHEMA = "CREATE SCHEMA IF NOT EXISTS %s";
    public static final String CHANGE_LOG_PATH = "classpath:config/liquibase/master.xml";
    public static final String LOGIN_IS_USED_ERROR_TEXT = "Login already in use";
    public static final String CREATE_PROFILE_EVENT_TYPE = "CREATE_PROFILE";
    public static final String UPDATE_PROFILE_EVENT_TYPE = "UPDATE_PROFILE";
    public static final String UPDATE_ACCOUNT_EVENT_TYPE = "UPDATE_ACCOUNT";

    public static final String CERTIFICATE = "X.509";
    public static final String KEYSTOPE_TYPE = "PKCS12";
    public static final String KEYSTORE_FILE = "keystore.p12";

    //System event data properties
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String IMAGE_URL = "imageUrl";
    public static final String CREATED_DATE = "createdDate";
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";
    public static final String USER_KEY = "userKey";

    public static final String DEFAULT_CONFIG_PATH = "config/specs/default-uaa.yml";
    public static final String DEFAULT_LOGINS_CONFIG_PATH = "config/specs/default-logins.yml";

    private Constants() {
    }
}

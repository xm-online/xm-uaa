package com.icthh.xm.uaa.service;

/**
 * Describes a service that provides {@link PermissionsConfigMode} to be used.
 */
public interface PermissionsConfigModeProvider {

    /**
     * @return {@link PermissionsConfigMode} to be used.
     */
   PermissionsConfigMode getMode();
}

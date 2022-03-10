/*
 * Copyright 2018-2021 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.vault.vaultSecretStore;

import org.forgerock.openam.plugins.AmPlugin;
import org.forgerock.openam.plugins.PluginException;
import org.forgerock.openam.plugins.PluginTools;
import org.forgerock.openam.plugins.StartupType;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.config.GlobalSecrets;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;

import javax.inject.Inject;

/**
 * An AM plugin for vault secrets.
 */
public class VaultSecretsPlugin implements AmPlugin {

    private final PluginTools pluginTools;
    private final AnnotatedServiceRegistry serviceRegistry;
    private final Secrets secrets;
    private final SecretsIdRegistry secretIdRegistry;

    /**
     * Guice-injected constructor.
     * @param pluginTools The plugin tools.
     * @param serviceRegistry The service registry.
     * @param secrets The secrets service.
     * @param secretIdRegistry The secret ID registry.
     */
    @Inject
    public VaultSecretsPlugin(PluginTools pluginTools, AnnotatedServiceRegistry serviceRegistry, Secrets secrets,
            SecretsIdRegistry secretIdRegistry) {
            this.pluginTools = pluginTools;
            this.serviceRegistry = serviceRegistry;
            this.secrets = secrets;
            this.secretIdRegistry = secretIdRegistry;
    }

    @Override
    public String getPluginVersion() {
        return "0.0.19";
    }

    @Override
    public void upgrade(String fromVersion) throws PluginException {
    }

    @Override
    public void onInstall() throws PluginException {
        pluginTools.installService(GlobalSecrets.class);
        pluginTools.installSecretsStoreType(VaultSecretStore.class);
    }

    @Override
    public void onStartup(StartupType startupType) throws PluginException {
        onStartup(startupType, true);
    }

    public void onStartup(StartupType startupType, boolean createMappingsToGeneratedKeys) throws PluginException {
        secretIdRegistry.registerSingletonSecretIds();
        pluginTools.startService(GlobalSecrets.class);
        pluginTools.startSecretsStoreType(VaultSecretStore.class);
    }

}
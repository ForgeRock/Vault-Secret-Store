//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.vault.vaultSecretStore;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.forgerock.openam.secrets.SecretIdProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Set;

@Singleton
public class SecretsIdRegistry {
    private final Multimap<String, String> globalSecretIds = MultimapBuilder.treeKeys().treeSetValues().build();
    private final Multimap<String, String> realmSecretIds = MultimapBuilder.treeKeys().treeSetValues().build();
    private final Set<SecretIdProvider> secretIdProviders;

    @Inject
    public SecretsIdRegistry(Set<SecretIdProvider> secretIdProviders) {
        this.secretIdProviders = secretIdProviders;
    }

    void registerSingletonSecretIds() {
        Iterator var1 = this.secretIdProviders.iterator();

        while(var1.hasNext()) {
            SecretIdProvider secretIdProvider = (SecretIdProvider)var1.next();
            this.globalSecretIds.putAll(secretIdProvider.getGlobalSingletonSecretIds());
            this.globalSecretIds.putAll(secretIdProvider.getRealmSingletonSecretIds());
            this.realmSecretIds.putAll(secretIdProvider.getRealmSingletonSecretIds());
        }

    }
}

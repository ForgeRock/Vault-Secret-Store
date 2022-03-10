/*
 * Copyright 2018-2021 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package com.vault.vaultSecretStore;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.json.JsonPointer;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.annotations.sm.Config;
import org.forgerock.openam.annotations.sm.Id;
import org.forgerock.openam.annotations.sm.SubConfig;
import org.forgerock.openam.secrets.SecretException;
import org.forgerock.openam.secrets.SecretStoreContext;
import org.forgerock.openam.secrets.SimpleSecretStoreProvider;
import org.forgerock.openam.secrets.config.PurposeMapping;
import org.forgerock.openam.secrets.config.SingleAliasPurposeMappingValidator;
import org.forgerock.openam.sm.annotations.subconfigs.Multiple;
import org.forgerock.secrets.*;
import org.forgerock.secrets.keys.SigningKey;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.secrets.vault.VaultConfig;
import org.forgerock.secrets.vault.VaultKeyValueSecretStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.forgerock.secrets.GenericSecret.password;
import static org.forgerock.secrets.Purpose.purpose;
import static org.forgerock.secrets.SecretReference.constant;


@Config(scope = Config.Scope.REALM_AND_GLOBAL, collection = true)
public interface VaultSecretStore extends SimpleSecretStoreProvider {
    Logger logger = LoggerFactory.getLogger(VaultSecretStore.class);

    @Id
    String id();

    /**
     * The base URI used to access vault.
     *
     * @return the base URI of the vault to use.
     */
    @Attribute(order = 100, requiredValue = true)
    default String vault_base_uri() {
        return "http://127.0.0.1:8200";
    }

    /**
     * The namespace of the vault.
     *
     * @return the namespace of the vault to use.
     */
    @Attribute(order = 200)
    default String vault_namespace() {
        return "\"\"";
    }

    /**
     * The token of the vault.
     *
     * @return the token of the vault to use for authentication.
     */
    @Attribute(order = 300, requiredValue = true)
    String vault_token();

    @SubConfig(validator = SingleAliasPurposeMappingValidator.class)
    Multiple<PurposeMapping> mappings();


    /**
     * Builds the Vault to use as a secret store
     *
     * @return the VaultKeyValueSecretStore.
     */
    @Override
        default SecretStore<Secret> getStore(SecretStoreContext context) throws SecretException {


        Map<JsonPointer, VaultKeyValueSecretStore.SecretFieldDecoder> mapping1 = Map.of(
                new JsonPointer("metadata/version"), VaultKeyValueSecretStore.SecretField.STABLE_ID,
                new JsonPointer("data/secret"), VaultKeyValueSecretStore.SecretField.RAW_DATA_BASE64,
                new JsonPointer("data/key"), VaultKeyValueSecretStore.SecretField.PEM,
                new JsonPointer("data/cert"), VaultKeyValueSecretStore.SecretField.PEM
        );
        
        SecretReference<GenericSecret> vaultToken = constant(password(vault_token().toCharArray()));
        VaultConfig config = null;


        Map<Purpose<?>, String> keyMappings = new LinkedHashMap<>();
        Multiple<PurposeMapping> purposeMappings = mappings();
        List<PurposeMapping> mappings = new ArrayList<>();
        try {
            for (String purposeId : purposeMappings.idSet()) {
                mappings.add(purposeMappings.get(purposeId));
            }
        } catch (SMSException e) {
            e.printStackTrace();
        } catch (SSOException e) {
            e.printStackTrace();
        }

        for (PurposeMapping mapping : mappings) {
            List<String> keyNames = mapping.aliases();
            Purpose<SigningKey> signingPurpose = purpose(mapping.secretId(), SigningKey.class);
            Purpose<VerificationKey> verificationPurpose = purpose(mapping.secretId(), VerificationKey.class);
            keyMappings.put(signingPurpose, keyNames.get(0));
            keyMappings.put(verificationPurpose, keyNames.get(0));
        }

        try {
            config = VaultConfig.builder(new HttpClientHandler(), URI.create(vault_base_uri()))
                    .namespace(vault_namespace())
                    .clock(Clock.systemUTC())
                    .purposeMapping(keyMappings)
                    .build();
        } catch (HttpApplicationException e) {
            e.printStackTrace();
        }

        return new VaultKeyValueSecretStore(vaultToken, mapping1, config);
    }


}
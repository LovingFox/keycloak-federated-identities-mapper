package dev.keycloak.mapper;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FederatedIdentitiesMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "federated-identities-mapper";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        // Adds the "Token Claim Name" field
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);

        // Adds the "Add to ID token", "Add to access token", and "Add to userinfo" toggles
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, FederatedIdentitiesMapper.class);

        // Add multi-valued configuration option
        ProviderConfigProperty multiValued = new ProviderConfigProperty();
        multiValued.setName("multivalued");
        multiValued.setLabel("Multi-valued");
        multiValued.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        multiValued.setDefaultValue("true");
        multiValued.setHelpText("If true, the claim will be a JSON array; if false, it will be a single object.");
        configProperties.add(multiValued);

        // Add identity providers filter configuration option
        ProviderConfigProperty identityProvidersFilter = new ProviderConfigProperty();
        identityProvidersFilter.setName("identityProvidersFilter");
        identityProvidersFilter.setLabel("Identity Providers to Map");
        identityProvidersFilter.setType(ProviderConfigProperty.STRING_TYPE);
        identityProvidersFilter.setHelpText("Comma-separated list of identity provider aliases to map (e.g., vkid,yandex). Leave empty to map all.");
        configProperties.add(identityProvidersFilter);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Federated Identities Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds federated identities of the user to the token as a JSON array, optionally filtered by identity providers.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession,
                            KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        UserModel user = userSession.getUser();
        String claimName = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        if (claimName == null || claimName.isEmpty()) {
            claimName = "federated_identities";
        }

        List<Map<String, String>> federatedIdentities = new ArrayList<>();
        List<IdentityProviderModel> identityProviders = keycloakSession.getContext().getRealm().getIdentityProvidersStream().toList();
        String filterConfig = mappingModel.getConfig().get("identityProvidersFilter");
        Set<String> filterProviders = new HashSet<>();
        if (filterConfig != null && !filterConfig.trim().isEmpty()) {
            filterProviders = Arrays.stream(filterConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }

        for (IdentityProviderModel idp : identityProviders) {
            FederatedIdentityModel federatedIdentity = keycloakSession.users().getFederatedIdentity(keycloakSession.getContext().getRealm(), user, idp.getAlias());
            if (federatedIdentity != null) {
                if (filterProviders.isEmpty() || filterProviders.contains(idp.getAlias())) {
                    Map<String, String> identityMap = new HashMap<>();
                    identityMap.put("identityProvider", federatedIdentity.getIdentityProvider());
                    identityMap.put("userId", federatedIdentity.getUserId());
                    identityMap.put("userName", federatedIdentity.getUserName());
                    federatedIdentities.add(identityMap);
                }
            }
        }

        // Ensure the claim is treated as multi-valued
        boolean isMultiValued = Boolean.parseBoolean(mappingModel.getConfig().getOrDefault("multivalued", "true"));
        if (isMultiValued && !federatedIdentities.isEmpty()) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, federatedIdentities);
        } else if (!federatedIdentities.isEmpty()) {
            // Fallback to single object if multi-valued is false or list is empty
            Map<String, String> singleIdentity = federatedIdentities.get(0);
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, singleIdentity);
        }
    }
}

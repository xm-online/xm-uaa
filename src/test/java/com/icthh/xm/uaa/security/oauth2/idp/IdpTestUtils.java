package com.icthh.xm.uaa.security.oauth2.idp;

import com.icthh.xm.commons.domainevent.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domainevent.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domainevent.idp.model.IdpPublicConfig.IdpConfigContainer.Features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class IdpTestUtils {

    public static IdpPublicConfig buildPublicConfig(String clientKeyPrefix,
                                                    String clientId, String issuer, int clientsAmount,
                                                    boolean buildValidConfig) {
        if (!buildValidConfig) {
            return new IdpPublicConfig();
        }
        IdpPublicConfig idpPublicConfig = new IdpPublicConfig();
        IdpPublicConfig.IdpConfigContainer config = new IdpPublicConfig.IdpConfigContainer();

        config.setDirectLogin(true);

        List<IdpPublicClientConfig> idpPublicClientConfigs = new ArrayList<>();
        for (int i = 0; i < clientsAmount; i++) {
            idpPublicClientConfigs.add(buildIdpPublicClientConfig(clientKeyPrefix + i, clientId + i, issuer));
        }
        config.setClients(idpPublicClientConfigs);
        config.setFeatures(buildFeatures());

        idpPublicConfig.setConfig(config);

        return idpPublicConfig;
    }

    public static IdpPublicClientConfig buildIdpPublicClientConfig(String clientKey, String clientId, String issuer) {
        IdpPublicClientConfig idpPublicClientConfig = new IdpPublicClientConfig();
        IdpPublicClientConfig.OpenIdConfig openIdConfig = new IdpPublicClientConfig.OpenIdConfig();

        idpPublicClientConfig.setKey(clientKey);
        idpPublicClientConfig.setClientId(clientId);
        idpPublicClientConfig.setName(clientKey);
        idpPublicClientConfig.setRedirectUri("http://localhost:4200");

        openIdConfig.setIssuer(issuer);
        openIdConfig.setAuthorizationEndpoint(buildAuthorizationEndpoint());
        openIdConfig.setTokenEndpoint(buildTokenEndpoint());
        openIdConfig.setUserinfoEndpoint(buildUserInfoEndpoint());
        openIdConfig.setEndSessionEndpoint(buildEndSessionEndpoint());
        openIdConfig.setJwksEndpoint(buildJwksEndpoint());

        idpPublicClientConfig.setOpenIdConfig(openIdConfig);

        return idpPublicClientConfig;
    }

    private static IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint buildUserInfoEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/userinfo");
        userinfoEndpoint.setUserNameAttributeName("email");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildEndSessionEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/logout");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildJwksEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/.well-known/jwks.json");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.TokenEndpoint buildTokenEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.TokenEndpoint tokenEndpoint = new IdpPublicClientConfig.OpenIdConfig.TokenEndpoint();
        tokenEndpoint.setUri("https://idp1.com/oauth/token");
        tokenEndpoint.setGrantType("authorization_code");
        return tokenEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint buildAuthorizationEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint authorizationEndpoint = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint();

        authorizationEndpoint.setUri("https://idp1.com/authorize");
        authorizationEndpoint.setResponseType("code");
        authorizationEndpoint.setAdditionalParams(Map.of("connection", "google-oauth2"));

        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features features = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features();
        features.setState(true);
        authorizationEndpoint.setFeatures(features);

        return authorizationEndpoint;
    }

    private static Features buildFeatures() {
        Features features = new Features();

        features.setPkce(false);
        features.setStateful(false);

        Features.IdpAccessTokenInclusion idpAccessTokenInclusion = new Features.IdpAccessTokenInclusion();

        idpAccessTokenInclusion.setEnabled(true);
        idpAccessTokenInclusion.setIdpTokenHeader("Authorization");
        idpAccessTokenInclusion.setXmTokenHeader("X-Authorization");

        features.setIdpAccessTokenInclusion(idpAccessTokenInclusion);

        return features;
    }

    public static Map<String, List<Map<String, Object>>> buildJWKS(boolean buildValidConfig) {
        if (!buildValidConfig) {
            return new HashMap<>();
        }

        Map<String, Object> key1 = Map.of(
            "alg", "RS256",
            "kty", "RSA",
            "use", "sig",
            "n", "rIyUVO6vbRWiDxIP3sRpMw1BPeMxc0pUHJkgNly-dwovwK5opvFCRbVTbBZz99gC2LxJhWGha65D8Tcc0ibSvNyb_" +
                "Fho95Xyh0EGFJwiweA398c2A9M025_nlSeQx_IhDmmR_JDaCxz1oyEvnX9qcPSJbtj9WQJj62qJdmnX_k3nutI6GrwgMzFJkO" +
                "hlvCT3_q_DjoejoBGGd9k-ng791DNNNErqyhND8-4Te8KraoZ3ZdE2Pgdbr3dij81Q8GlQDiZF9g58mFNgIQuu0C1VWtLR_Hs" +
                "hXE2AwxbJBr3m85BNDFsBuFVMQVbCGnFX1yHzJhYHBO3dDBrLjKyBKYyZSw",
            "e", "AQAB",
            "kid", "ozRd0JdbWHAmxvtcbqpxX",
            "x5t", "HJSdOez5P4uzbv23t99EomHYKG4",
            "x5c", List.of("MIIDBzCCAe+gAwIBAgIJI28PgIro0xHLMA0GCSqGSIb3DQEBCwUAMCExHzAdBgNVBAMTFnVuaXQtdGVzdC5l" +
                "dS5hdXRoMC5jb20wHhcNMjEwMjIzMDkwODU5WhcNMzQxMTAyMDkwODU5WjAhMR8wHQYDVQQDExZ1bml0LXRlc3QuZXUuYXV0aDAu" +
                "Y29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArIyUVO6vbRWiDxIP3sRpMw1BPeMxc0pUHJkgNly+dwovwK5opvF" +
                "CRbVTbBZz99gC2LxJhWGha65D8Tcc0ibSvNyb/Fho95Xyh0EGFJwiweA398c2A9M025/nlSeQx/IhDmmR/JDaCxz1oyEvnX9qc" +
                "PSJbtj9WQJj62qJdmnX/k3nutI6GrwgMzFJkOhlvCT3/q/DjoejoBGGd9k+ng791DNNNErqyhND8+4Te8KraoZ3ZdE2Pgdbr3d" +
                "ij81Q8GlQDiZF9g58mFNgIQuu0C1VWtLR/HshXE2AwxbJBr3m85BNDFsBuFVMQVbCGnFX1yHzJhYHBO3dDBrLjKyBKYyZSwID" +
                "AQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTq7fwObMTSZb0J7p2LOTDHBWYNpTAOBgNVHQ8BAf8EBAMCAoQwD" +
                "QYJKoZIhvcNAQELBQADggEBAALRUqWwy1aIBxJPFAWjveErSi3ZwUsRIf6Ei/mL+rmkRDKcM/VW2i6y0GOxXUj8TS8rLAgh2Q7P" +
                "p3/RxlvU4e43HIWoUHekIitlgxAQ6KJSHjlxGdU0asSnSks2P4FmTB7SpnlI+ni6RJUtSydUrSIjQ5jhgxG/TTZEJcfTO0FMEP" +
                "il+97dqLB2ppgEtYUtjiDfjiRaFHzwu66+NGNky3AMuBtgsMtgunebnTpznangm/A/rLDGBu3cNFpojgVQkM88sBR2V1oIRXT+" +
                "6JR4Etp9KXNEI049XcRtQbq1MkGSpI/wUUc6g8CgoTA/R9qkhqlV/C2skoBd3sd5HF8jzGA="
            )
        );

        Map<String, Object> key2 = Map.of(
            "alg", "RS256",
            "kty", "RSA",
            "use", "sig",
            "n", "wfIhmn0D7fLi0NA-wrJMsH3fw-gSZ9kGzMvk4jTvJh2KInEuxqgwlGdKVZ0JMukiWuWH945sayrJQZICbeIchxzJW" +
                "80qC9hjYIBWo1QG0XN9nAJIC9onehKjOXHu7dLNKErLIFyZ6isiQ2Tm47SQ_NpSHGxnquz-4O7mjhsEHmXuYdiXeSl-uRgDQK" +
                "OpOYxjIEeX3hi1Xk47ioTz6Nr0y9joIB8f8Td8SWxqcLKsAmlAGeMUl4X6ClecuSqZmQo1kJgxOGZ8I1-5G1z7eKLG7bxtbRC" +
                "7zFwYmGtMLbHDvZW7qHQPdiA-X-R36nH8ZFYkSJ16c4fzj2LYa-m_GBGQIQ",
            "e", "AQAB",
            "kid", "ix9AaOmcho5lbqfCpg8SC",
            "x5t", "egw-WDErENHgEex9UItRDq_cyDo",
            "x5c", List.of("MIIDBzCCAe+gAwIBAgIJcr/e8RFtQXq/MA0GCSqGSIb3DQEBCwUAMCExHzAdBgNVBAMTFnVuaXQtdGVzdC5l" +
                "dS5hdXRoMC5jb20wHhcNMjEwMjIzMDkwODU5WhcNMzQxMTAyMDkwODU5WjAhMR8wHQYDVQQDExZ1bml0LXRlc3QuZXUuYXV0aDA" +
                "uY29tMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwfIhmn0D7fLi0NA+wrJMsH3fw+gSZ9kGzMvk4jTvJh2KInEuxq" +
                "gwlGdKVZ0JMukiWuWH945sayrJQZICbeIchxzJW80qC9hjYIBWo1QG0XN9nAJIC9onehKjOXHu7dLNKErLIFyZ6isiQ2Tm47SQ/" +
                "NpSHGxnquz+4O7mjhsEHmXuYdiXeSl+uRgDQKOpOYxjIEeX3hi1Xk47ioTz6Nr0y9joIB8f8Td8SWxqcLKsAmlAGeMUl4X6Clec" +
                "uSqZmQo1kJgxOGZ8I1+5G1z7eKLG7bxtbRC7zFwYmGtMLbHDvZW7qHQPdiA+X+R36nH8ZFYkSJ16c4fzj2LYa+m/GBGQIQIDAQA" +
                "Bo0IwQDAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBQN6il6bbM8/HhRUTXlU373Oa5GojAOBgNVHQ8BAf8EBAMCAoQwDQYJKo" +
                "ZIhvcNAQELBQADggEBALGV5XtYjUXEbwH91iLva0epitCIwnISeLzXCbczoXN9bVYtKegUWq/8gUhOM07FH+xXf/vFoqhCgAbBDd" +
                "WWsNLkA0+0M8Qnj1/VmdtqBxDfOiPKThtTxNh1P74+FBSgDVf5+5Pl/z1p3By0E4DAf6IjlLe73MOW2k3Br+7ACEnrh6g2Tvu0x" +
                "xaGVN00MU96IEH9kiYm80WlHnJhE3nctHRpe4FnKKTlxLUYYbEEkQvReo+QbHbSEWbXidFCbTz1Nmy8dCuKULob0OFG4VfBbVEI" +
                "96z5X9cddEqpyRHsxUWzZT+RCTy0bu3LjbfkldZa2pNUf1g6n4L+mJ7OsLCrUpc="
            )
        );

        return Map.of("keys", List.of(key1, key2));
    }

    public static String getIdToken() {
        return "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Im96UmQwSmRiV0hBbXh2dGNicXB4WCJ9.eyJnaXZlbl9uYW1lIjoiZG" +
            "V2IiwiZmFtaWx5X25hbWUiOiJ0ZXN0Iiwibmlja25hbWUiOiJkZXZ0ZXN0MDQ2IiwibmFtZSI6ImRldiB0ZXN0IiwicGljdHVyZSI6" +
            "Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS8tdmVxTmVjTFJHNjQvQUFBQUFBQUFBQUkvQUFBQUFBQUFBQUEvQU1adX" +
            "Vja0RheFBfdHpjQVc1LU9NNmJtSzhtQmZybHJwdy9zOTYtYy9waG90by5qcGciLCJsb2NhbGUiOiJlbiIsInVwZGF0ZWRfYXQiOiIy" +
            "MDIxLTAyLTIzVDA5OjM1OjA2LjI0N1oiLCJlbWFpbCI6ImRldnRlc3QwNDZAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydW" +
            "UsImlzcyI6Imh0dHBzOi8vdW5pdC10ZXN0LmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEwNTUxNzM4NTM0NDM2" +
            "MjE1NzIyMyIsImF1ZCI6Ikk0aDV2bkFFRHdYQW5rcHVuMmI5bXEzYnl3SHNwNzF3IiwiaWF0IjoxNjE0MDcyOTQ4LCJleHAiOjE2MT" +
            "QxMDg5NDgsIm5vbmNlIjoiV0djNFltWHhZYVdrM3djUUtWUEtsdkZ6cGVTc0RXaGlmVVNVLURSZkRMdyJ9.UfQVlAG4-3nSNVRbRIrX" +
            "wDskiWz6LobRqG-0XJwMDho5CN76ZgxVdj7OLq3cABuyOP_I7y5ctkMAWiLo0CPom8dqdZ6-5SMk67rbNNlnQD2rLU9I2QSDMjt-f_" +
            "gO_VtzLOQO0zArTYtWBF3r3TH_adaY1pb-E0fJHXXedy0-yyJV-5F96kZy4XDWNYYPluKgydSBeGVa9RNonTuVKcZkdUNveOe2iU-" +
            "F0XBKmyT5uLgdh2D-k1fLb_KudtAEdv58r7FW5YVGGpJPwjnwzUXtLTiCQU0CWLxYGdC-9hLYutlTUBaKEiClaFNnQyonq1kA_Cub7" +
            "ts-KE-CvcqQrj2H8Q";
    }
}

package com.narvar.tools;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.narvar.tools.jsonpojo.RetailerInfoJson;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 *
 */
public class UpdateRetailerWithApiConfig {

    private static final Connection POSTGRES_CONNECTION;
    private static final String SALT_VALUE = "salt";
    private static final String CIPHER_VALUE = "cipher";

    public static final String ALLOWED_APIS_JSON_KEY = "allowed_apis";

    // API access token
    public static final String PRIMARY_TOKEN_ID = "1";
    public static final String SECONDARY_TOKEN_ID = "2";
    public static final String AUTHENTICATION_TOKEN_CREATED_SUCCESSFULLY = "company.api_token_creation_success";
    public static final String ACCESS_TOKEN_JSON_KEY = "access_token";
    public static final String ACCESS_TOKENS_JSON_KEY = "access_tokens";
    public static final String API_CONFIG_JSON_KEY = "api_config";
    public static String ENCRYPTION_PASSWORD;

    static {
        POSTGRES_CONNECTION = PostgresConnection.getPostgresConnection();
        ENCRYPTION_PASSWORD = PasswordRetriever.encyptionPasswordFromGlobalProperties(POSTGRES_CONNECTION);
    }

    public static void main(String args[]) throws IOException, JSONException {

        PreparedStatement st;
        ResultSet rs;
        Long retailerId;
        String uriMoniker;
        String retailerInfoJson;
        String accountKey;
        try {
            st = POSTGRES_CONNECTION.prepareStatement("SELECT dbio_rowid, uri_moniker,retailer_info_json, account_key from retailer_info ri");
            rs = st.executeQuery();

//            st = POSTGRES_CONNECTION.prepareStatement("SELECT dbio_rowid, uri_moniker,retailer_info_json, account_key from retailer_info ri where ri.dbio_rowid in (?, ?)");
//            st.setInt(1, 4653);
//            st.setInt(2, 133);
//            rs = st.executeQuery();

            while (rs.next()) {
                retailerId = rs.getLong(1);
                uriMoniker = rs.getString(2);
                retailerInfoJson = rs.getString(3);
                accountKey = rs.getString(4);
                String assignedAccountKey = assignAccountKeyIfMissing(accountKey);
                Map<String, RetailerInfoJson.AccessToken> tokenNameToAccessToken = retrieveTokensMapForTenant(retailerInfoJson);
                String tokenId = PRIMARY_TOKEN_ID;
                Map<String, RetailerInfoJson.AccessToken> updateTokensMapForTokenId = updateTokensMapForTokenId(tokenNameToAccessToken, tokenId);
                tokenId = SECONDARY_TOKEN_ID;
                updateTokensMapForTokenId = updateTokensMapForTokenId(updateTokensMapForTokenId, tokenId);
//                System.out.println("New Map");
//                tokenNameToAccessToken.entrySet().forEach(System.out::println);
                String updatedRetailerJsonString = updateRetailerJsonStringWithTokensMap(retailerInfoJson, updateTokensMapForTokenId);
//                System.out.println("updatedRetailerJsonString= " + updatedRetailerJsonString);
                if (!Objects.equals(updatedRetailerJsonString, retailerInfoJson) || !Objects.equals(accountKey, assignedAccountKey)) {
                    saveUpdates(updatedRetailerJsonString, assignedAccountKey, retailerId);
                }
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    private static Map<String, RetailerInfoJson.AccessToken> updateTokensMapForTokenId(Map<String, RetailerInfoJson.AccessToken> tokenNameToAccessToken, String tokenId) throws JSONException {
        String newRawToken = UUID.randomUUID().toString().replaceAll("-", "");
        final String newEncryptedToken = encryptPII(newRawToken);
        tokenNameToAccessToken.computeIfAbsent(tokenId, k -> new RetailerInfoJson.AccessToken(newEncryptedToken));
        return tokenNameToAccessToken;
    }

    private static void saveUpdates(String updatedRetailerJsonString, String assignedAccountKey, Long retailerId) throws SQLException {
        PreparedStatement st = POSTGRES_CONNECTION.prepareStatement("UPDATE retailer_info SET retailer_info_json = ?, account_key = ? WHERE dbio_rowid = ?");
        st.setString(1, updatedRetailerJsonString);
        st.setString(2, assignedAccountKey);
        st.setLong(3, retailerId);
        st.execute();
    }

    /**
     * Method is used to encrypt the plain text
     *
     * @param plainText: The plain text to be encrypted
     * @return encryptedString: JSON String in the format of { "cipher": "<cipher text>", "salt": "<salt used in encryption>"}
     */
    public static String encryptPII(String plainText) throws JSONException {
        final String salt = KeyGenerators.string().generateKey();
        String encryptedString = null;
        if (ENCRYPTION_PASSWORD != null && plainText != null) {
            com.amazonaws.util.json.JSONObject encryptedJSONObj = new com.amazonaws.util.json.JSONObject();
            TextEncryptor encryptor = Encryptors.text(ENCRYPTION_PASSWORD, salt);
            String encryptedText = encryptor.encrypt(plainText);
            encryptedJSONObj.put(CIPHER_VALUE, encryptedText);
            encryptedJSONObj.put(SALT_VALUE, salt);
            encryptedString = encryptedJSONObj.toString();
        }
        return encryptedString;
    }

    public static String decryptCipherText(String password, String cipherEncryptedText) throws JSONException {
        String plainText = null;
        if (password != null) {
            JSONObject cipherJSONObj = new JSONObject(cipherEncryptedText);
            String encryptedText = cipherJSONObj.getString(CIPHER_VALUE);
            String salt = cipherJSONObj.getString(SALT_VALUE);
            TextEncryptor decryptor = Encryptors.text(password, salt);
            plainText = decryptor.decrypt(encryptedText);
        }
        return plainText;
    }

    /**
     * Generate accountKey if missing
     *
     * @param accountKey
     * @return
     */
    public static String assignAccountKeyIfMissing(final String accountKey) {
        String updatedAccountKey = accountKey;
        if (accountKey == null || accountKey.isEmpty()) {
            updatedAccountKey = UUID.randomUUID().toString().replaceAll("-", "");
        }
        return updatedAccountKey;
    }

    private static Map<String, RetailerInfoJson.AccessToken> retrieveTokensMapForTenant(String retailerInfoJsonString) throws IOException, SQLException, JSONException {
        Map<String, RetailerInfoJson.AccessToken> tokenNameToAccessToken = new HashMap<>();
        if (retailerInfoJsonString != null && !retailerInfoJsonString.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonFactory factory = objectMapper.getFactory();
            JsonParser jp = factory.createParser(retailerInfoJsonString);
            JsonNode jsonTree = objectMapper.readTree(jp);
            Iterator<Map.Entry<String, JsonNode>> accessTokenEntries = jsonTree.path(API_CONFIG_JSON_KEY).path(ACCESS_TOKENS_JSON_KEY).fields();
            while (accessTokenEntries.hasNext()) {
                Map.Entry<String, JsonNode> tokenEntry = accessTokenEntries.next();
                final String tokenName = tokenEntry.getKey();
                final String textValue = tokenEntry.getValue().path(ACCESS_TOKEN_JSON_KEY).textValue();
//                String decryptCipherText = decryptCipherText(ENCRYPTION_PASSWORD, textValue);
//                System.out.println("key= " + tokenName + " has decryptCipherText= " + decryptCipherText);
                tokenNameToAccessToken.put(tokenName, new RetailerInfoJson.AccessToken(textValue));
            }
        }
        return tokenNameToAccessToken;
    }

    private static String updateRetailerJsonStringWithTokensMap(String retailerInfoJsonString, Map<String, RetailerInfoJson.AccessToken> tokenNameValuePair) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> outerMap;
        if (retailerInfoJsonString == null || retailerInfoJsonString.isEmpty()) {
            outerMap = new HashMap<>();
            outerMap = updateOuterMapWithNewApiConfigKey(tokenNameValuePair, outerMap);
        } else {
            outerMap = objectMapper.readValue(retailerInfoJsonString, new TypeReference<Map<String, Object>>() {
            });
            if (outerMap.containsKey(API_CONFIG_JSON_KEY)) {
                Object innerMap1 = outerMap.get(API_CONFIG_JSON_KEY);
                if ((innerMap1 != null) && (innerMap1 instanceof Map)) {
                    Map<String, ?> castedInnerMap1 = (Map) innerMap1;
                    if (!castedInnerMap1.isEmpty()) {
                        Object accessTokensMap = castedInnerMap1.get(ACCESS_TOKENS_JSON_KEY);
                        if ((accessTokensMap != null) && accessTokensMap instanceof Map) {     // append to access_tokens map
                            ((Map) ((Map) ((Map) outerMap.get(API_CONFIG_JSON_KEY))).get(ACCESS_TOKENS_JSON_KEY)).putAll(tokenNameValuePair);
                        } else {
                            // Create key "access_tokens" JSON KEY with value tokenNameToAccessToken
                            Map<String, Map<String, ?>> newInnerMap2 = new HashMap<>();
                            newInnerMap2.put(ACCESS_TOKENS_JSON_KEY, tokenNameValuePair);
                            // Add the newly created "access_tokens" inner map to the "api_config" map
                            ((Map) outerMap.get(API_CONFIG_JSON_KEY)).putAll(newInnerMap2);
                        }
                    } else {
                        // Create "api_config" -> "access_tokens" -> tokenNameValuePair" then put it in the outer map
                        outerMap = updateOuterMapWithNewApiConfigKey(tokenNameValuePair, outerMap);
                    }
                } else {
                    // Create "api_config" -> "access_tokens" -> tokenNameValuePair" then put it in the outer map
                    outerMap = updateOuterMapWithNewApiConfigKey(tokenNameValuePair, outerMap);
                }
            } else {
                // Create "api_config" -> "access_tokens" -> tokenNameValuePair" then put it in the outer map
                outerMap = updateOuterMapWithNewApiConfigKey(tokenNameValuePair, outerMap);
            }
        }
        retailerInfoJsonString = objectMapper.writeValueAsString(outerMap);
        return retailerInfoJsonString;
    }

    /**
     * Create "api_config" -> "access_tokens" -> tokenNameValuePair" then put it
     * in the outer map
     *
     * @param tokenNameValuePair
     * @param outerMap
     * @return
     */
    private static Map<String, Object> updateOuterMapWithNewApiConfigKey(Map<String, RetailerInfoJson.AccessToken> tokenNameValuePair, Map<String, Object> outerMap) {
        // initialize key ACCESS_TOKENS_JSON_KEY and create value with tokenNameToAccessToken map
        Map<String, Map<String, ?>> newAccessTokensMap = new HashMap<>();
        newAccessTokensMap.put(ACCESS_TOKENS_JSON_KEY, tokenNameValuePair);
        // initialize map with key API_CONFIG_JSON_KEY and value newInnerMap2
        Map<String, Map<String, ?>> newApiConfigMap = new HashMap<>();
        newApiConfigMap.put(API_CONFIG_JSON_KEY, newAccessTokensMap);
        ((Map) outerMap).putAll(newApiConfigMap);
        return outerMap;
    }
}

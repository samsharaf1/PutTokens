/**
 * 
 */
package com.narvar.tools.jsonpojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;


@JsonIgnoreProperties(ignoreUnknown = true)
public class RetailerInfoJson {

        @JsonProperty("api_config")
        private ApiConfig apiConfig;

        public ApiConfig getApiConfig() {
            return apiConfig;
        }

        public void setApiConfig(ApiConfig apiConfig) {
            this.apiConfig = apiConfig;
        }
        
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ApiConfig implements Serializable {

            private final static long serialVersionUID = -7042746695336369637L;

            public ApiConfig() {
                this.initialize();
            }
            @PostConstruct
            public void onCreation() {
                this.initialize();
            }
            private void initialize(){
                tokenNameToAccessToken = new HashMap<>();
                apiList = new ArrayList<>();
            }

            @JsonProperty("access_tokens")
            private Map<String, AccessToken> tokenNameToAccessToken;
            
            @JsonProperty("allowed_apis")
            private List<String> apiList;
            
            public List<String> getApiList() {
				return apiList;
			}
			public void setApiList(List<String> apiList) {
				this.apiList = apiList;
			}
			
            public Map<String, AccessToken> getTokenNameToAccessToken() {
                return tokenNameToAccessToken;
            }

            public void setTokenNameToAccessToken(Map<String, AccessToken> tokenNameToAccessToken) {
                this.tokenNameToAccessToken = tokenNameToAccessToken;
            }
        }
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class AccessToken implements Serializable {
            
            private final static long serialVersionUID = 8713998685821852965L;
            
            public AccessToken() {
            }
            
            public AccessToken(String accessToken) {
                this.tokenString = accessToken;
            }
            
            @JsonProperty(value = "access_token", required = true)
            private String tokenString;
            
            public String getTokenString() {
                return tokenString;
            }
            public void setTokenString(String tokenString) {
                this.tokenString = tokenString;
            }

            @Override
            public String toString() {
                return "AccessToken= " + tokenString;
            }
            
        }
}

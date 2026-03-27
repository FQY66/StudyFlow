package com.sf.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sf.jwt")
@Data
public class JwtProperties {

    /*密钥*/
    private String adminSecretKey;
    /**token过期时间*/
    private long adminTtl;
    private String adminTokenName;

}

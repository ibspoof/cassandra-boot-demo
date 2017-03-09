package com.github.ibspoof.cassandraclient.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
//@ConfigurationProperties(prefix="")
public class CassandraProperties {

    @Value("${cassandra.global.clusterName}")
    private String clusterName;


    @Value("${cassandra.global.reconnect.baseDelay}")
    private Integer reconnectBaseDelay;


    @Value("${cassandra.global.reconnect.maxDelay}")
    private Integer reconnectMaxDelay;


    @Value("${cassandra.local.hosts}")
    private String localHosts;

    @Value("${cassandra.local.port}")
    private Integer localPort;

    @Value("${cassandra.local.dcName}")
    private String localDcName;

    public String getClusterName() {
        return clusterName;
    }

    public Integer getReconnectBaseDelay() {
        return reconnectBaseDelay;
    }

    public Integer getReconnectMaxDelay() {
        return reconnectMaxDelay;
    }

    public String getLocalHosts() {
        return localHosts;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public String getLocalDcName() {
        return localDcName;
    }
}
package com.github.ibspoof.cassandraclient.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.policies.*;
import com.github.ibspoof.cassandraclient.configs.CassandraProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class CassandraSession {

    private static final String CHAR_COMMA = ",";

    private static final Logger logger = LoggerFactory.getLogger(CassandraSession.class.getSimpleName());

    @Autowired
    private CassandraProperties cassandraProperties;

    private static Session session;

    public CassandraSession(CassandraProperties cassandraProperties) {
        this.cassandraProperties = cassandraProperties;
        this.createSession();
    }

    public CassandraSession() {}

    public Session getSession( ) {
        return createSession();
    }

    @PostConstruct
    private Session createSession() {

        if (session != null) {
            return session;
        }

        Cluster.Builder clusterBuilder = Cluster.builder();

        /*
          Hosts should be in order of closest hosts (same DC) to farthest (remote DC).

          Max of 2-4 hosts hosts per DC are needed as the first one connected to will be used to
          determine all the clusters nodes and DCs
         */
        String[] hostList = this.cassandraProperties.getLocalHosts().split(CHAR_COMMA);
        clusterBuilder.addContactPoints(hostList).withPort(this.cassandraProperties.getLocalPort());

        clusterBuilder.withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                .setSerialConsistencyLevel(ConsistencyLevel.LOCAL_SERIAL));


        /*
          The single line method is deprecated, the builder is 2.1.10+ method

          If usedHostsPerRemoteDc > 0, then if for a query no host in the local datacenter can be reached and if the consistency
          level of the query is not LOCAL_ONE or LOCAL_QUORUM, then up to usedHostsPerRemoteDc hosts per remote
          datacenter will be tried by the policy as a fallback.

          By default, no remote host will be used for LOCAL_ONE and LOCAL_QUORUM, since this would change the meaning
          of the consistency level, somewhat breaking the consistency contract
          (this can be overridden with allowRemoteDCsForLocalConsistencyLevel()).


          If allowRemoteDCsForLocalConsistencyLevel() is used it allows the policy to return remote hosts when building
          query plans for queries having consistency level LOCAL_ONE or LOCAL_QUORUM.

          When used in conjunction with usedHostsPerRemoteDc > 0, this overrides the policy of
          never using remote datacenter nodes for LOCAL_ONE and LOCAL_QUORUM queries. It is however inadvisable to do
          so in almost all cases, as this would potentially break consistency guarantees and if you are fine with that,
          it's probably better to use a weaker consistency like ONE, TWO or THREE. As such, this method should generally be
          avoided; use it only if you know and understand what you do.
         */
        TokenAwarePolicy tokenAwarePolicy = new TokenAwarePolicy(
                DCAwareRoundRobinPolicy.builder()
                        .withLocalDc(cassandraProperties.getLocalDcName())
                        .withUsedHostsPerRemoteDc(0)
                        .build()
        );
        clusterBuilder.withLoadBalancingPolicy(tokenAwarePolicy);



        /*
          Start reconnect at 500ms and exponentially increase to 5 mins before stopping
          Looks like: 500ms -> 1s -> 2s -> 4s -> 8s -> 16s -> 32s -> 1m4s -> 2m8s -> 4m16s -> stop
         */
        clusterBuilder.withReconnectionPolicy(new ExponentialReconnectionPolicy(
               cassandraProperties.getReconnectBaseDelay(), this.cassandraProperties.getReconnectMaxDelay()));



        /*
          Set connection/read timeouts

          ConnectTimeoutMillis = 5seconds = default

          ReadTimeoutMillis =
          reduce this if needing to address lower SLAs than 12s, but expect higher number of timeouts
          it should be higher than the timeout settings used on the Cassandra side
         */
//        int connectTimeout = Integer.parseInt(config.getProperty(globalConfigPrefix + "socket.connectTimeoutMs"));
//        int readTimeout = Integer.parseInt(config.getProperty(globalConfigPrefix + "socket.readTimeoutMs"));
//        SocketOptions socketOptions = new SocketOptions()
//                .setConnectTimeoutMillis(connectTimeout)
//                .setReadTimeoutMillis(readTimeout);
//        clusterBuilder.withSocketOptions(socketOptions);
//


        /*
          Set local DC to use min 1 connection (34k threads) up to 3 max

          Set remote DC to use min 1 and max 1 connection
          If you set DCAwareRoundRobinPolicy.withUsedHostsPerRemoteDc(0) then
          setConnectionsPerHost(HostDistance.REMOTE, 0, 0) can be used to limit the number of connections to a node
          opened.

          With a large number of application hosts (50+) this can reduce the amount of concurrent connections to a
          single C* node.
         */

//        int minConns = Integer.parseInt(config.getProperty(globalConfigPrefix + "connections.min"));
//        int maxConns = Integer.parseInt(config.getProperty(globalConfigPrefix + "connections.max"));
//        PoolingOptions poolingOptions = new PoolingOptions()
//                .setConnectionsPerHost(HostDistance.LOCAL, minConns, maxConns)
//                .setConnectionsPerHost(HostDistance.REMOTE, 0, 0);
//        clusterBuilder.withPoolingOptions(poolingOptions);



        /*
          DefaultRetryPolicy.INSTANCE:
           - onReadTimeout = retry query same host
           - onWriteTimeout = retry query same host
           - onUnavailable = tryNextHost
           - onConnectionTimeout = tryNextHost

           Logging classes are to be included in your local logback.xml connection:
            {@link com.datastax.driver.core.policies.RetryPolicy.RetryDecision.Type#RETRY RETRY} and
            {@link com.datastax.driver.core.policies.RetryPolicy.RetryDecision.Type#IGNORE IGNORE} decisions (since
            {@link com.datastax.driver.core.policies.RetryPolicy.RetryDecision.Type#RETHROW RETHROW} decisions
         */
        clusterBuilder.withRetryPolicy(new LoggingRetryPolicy(DefaultRetryPolicy.INSTANCE));


        /*
         * Bubble up all exceptions from the driver to the app to chose what direction/action should be taken
         */
        clusterBuilder.withRetryPolicy(FallthroughRetryPolicy.INSTANCE);


        /*
        Add authentication if present
         */
//        String username = config.getProperty(globalConfigPrefix + "auth.username");
//        String password = config.getProperty(globalConfigPrefix + "auth.password");
//        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
//            clusterBuilder.withCredentials(username, password);
//        }



        /*
          Create connection
         */
        Cluster cluster = clusterBuilder.build();


        /*
          Create session/connect to all nodes with above settings
         */
        try {
           session =  cluster.connect();
        } catch (Exception e) {
            logger.error("Unable to connect to DSE.");
        }

        return session;
    }

}

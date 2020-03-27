package id.lmnzr.example.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    private static Logger log = Logger.getLogger(Main.class.getPackageName());

    public static void main(String[] args) {
        Config hazelcastConfig = new Config();
        JoinConfig joinConfig = hazelcastConfig.getNetworkConfig().getJoin();

        String hostAddress = null;
        try {
            hostAddress= getECSMetadata();
            log.info(String.format("Container Host Address %s", hostAddress));

            // Enable AWS Discovery
            hazelcastConfig.getGroupConfig().setName("my-vertx-cluster");
            hazelcastConfig.getNetworkConfig().getInterfaces().setEnabled(true).addInterface("10.0.*.*");
            joinConfig = hazelcastConfig.getNetworkConfig().getJoin();
            joinConfig.getMulticastConfig().setEnabled(false);
            joinConfig.getTcpIpConfig().setEnabled(false);
            joinConfig.getAwsConfig()
                    .setEnabled(true)
                    .setAccessKey("")
                    .setSecretKey("")
                    .setRegion("")
                    .setTagKey("aws:cloudformation:stack-name")
                    .setTagValue("EC2ContainerService-my-vertx-cluster")
            ;
        } catch (Exception e) {
            hostAddress = getAddress();
            log.info(String.format("Container Host Address %s", hostAddress));
        }

        hazelcastConfig.getNetworkConfig().setJoin(joinConfig);
        ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);

        VertxOptions options = new VertxOptions()
                .setClusterManager(mgr);

        EventBusOptions ebOptions = new EventBusOptions()
                .setHost(hostAddress)
                .setClustered(true);

        options.setEventBusOptions(ebOptions);

        Vertx.clusteredVertx(options, handler-> {
            if (handler.succeeded()) {
                handler.result().deployVerticle(RestVerticle.class,new DeploymentOptions().setInstances(1), deployHandler->{
                    if(handler.succeeded()){
                        log.info("Host Address "+ebOptions.getHost());
                        log.info("REST API Verticle Deployed");
                    }else{
                        log.severe("REST API Verticle Deployment Failed");
                    }
                });
            }
        });
    }

    // Function to get host address
    private static String getAddress()  {
        try {
            List<NetworkInterface> networkInterfaces = new ArrayList<>();
            NetworkInterface.getNetworkInterfaces().asIterator().forEachRemaining(networkInterfaces::add);
            return networkInterfaces.stream()
                    .flatMap(iface->iface.inetAddresses()
                        .filter(entry->entry.getAddress().length == 4)
                        .filter(entry->!entry.isLoopbackAddress())
                        .filter(entry->entry.getAddress()[0] != Integer.valueOf(10).byteValue())
                    .map(InetAddress::getHostAddress)).findFirst().orElse(null);
        } catch (SocketException e) {
            return null;
        }
    }

    private static String getECSMetadata() throws Exception {
        String responseString = null;
        try {
            URL url = new URL("http://169.254.169.254/latest/meta-data/local-ipv4");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            InputStream response = httpURLConnection.getInputStream();
            InputStreamReader isReader = new InputStreamReader(response);
            BufferedReader reader = new BufferedReader(isReader);
            StringBuilder sb = new StringBuilder();
            String str;
            while((str = reader.readLine())!= null){
                sb.append(str);
            }
            responseString = sb.toString();
        } catch (IOException e) {
            log.severe(e.getMessage());
            throw new Exception("Failed to retrieve Metadata");
        }
        return responseString;
    }
}


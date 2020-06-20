package org.egov.assets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.egov.tracer.config.TracerConfiguration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;


@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = {"org.egov.assets","org.egov.assets.common"})
@Import({TracerConfiguration.class})
public class AssetStoreManagement implements CommandLineRunner {

    private static final String CLUSTER_NAME = "cluster.name";

    @Value("${app.timezone}")
    private String timeZone;

//    @Value("${es.host}")
//    private String elasticSearchHost;
//
//    @Value("${es.transport.port}")
//    private Integer elasticSearchTransportPort;
//
//    @Value("${es.cluster.name}")
//    private String elasticSearchClusterName;

    private TransportClient client;

    @Override
    public void run(String... arg0) throws Exception {
        if (arg0.length > 0 && arg0[0].equals("exitcode")) {
            throw new ExitException();
        }
    }

//    @PostConstruct
//    public void init() throws UnknownHostException {
//        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
//        Settings settings = Settings.builder().put(CLUSTER_NAME, elasticSearchClusterName).build();
//        final InetAddress esAddress = InetAddress.getByName(elasticSearchHost);
//        final InetSocketTransportAddress transportAddress = new InetSocketTransportAddress(esAddress,
//                elasticSearchTransportPort);
//        client = new PreBuiltTransportClient(settings).addTransportAddress(transportAddress);
//    }

    public static void main(String[] args) throws Exception {
        new SpringApplication(AssetStoreManagement.class).run(args);
    }

    class ExitException extends RuntimeException implements ExitCodeGenerator {
        private static final long serialVersionUID = 1L;

        @Override
        public int getExitCode() {
            return 10;
        }

    }

    @Bean
    public TransportClient getTransportClient() {
        return client;
    }

}

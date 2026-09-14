package team.themoment.hellogsmv3.global.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan(basePackages = {"team.themoment.hellogsmv3.global.security.data",
        "team.themoment.hellogsmv3.global.thirdParty.aws.s3.data"})
public class PropertiesScanConfig {
}

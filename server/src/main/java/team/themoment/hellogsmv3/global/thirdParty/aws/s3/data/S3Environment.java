package team.themoment.hellogsmv3.global.thirdParty.aws.s3.data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
public record S3Environment(String bucketName) {
}

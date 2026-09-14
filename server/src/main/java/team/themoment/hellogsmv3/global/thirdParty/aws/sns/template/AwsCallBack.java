package team.themoment.hellogsmv3.global.thirdParty.aws.sns.template;

import java.io.IOException;

import io.awspring.cloud.s3.S3Exception;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

@FunctionalInterface
public interface AwsCallBack<T> {
    T execute() throws SdkClientException, S3Exception, AwsServiceException, IOException;
}

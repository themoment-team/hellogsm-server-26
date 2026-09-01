package team.themoment.hellogsmv3.global.thirdParty.aws.sns.template;

import java.io.IOException;

import org.springframework.stereotype.Component;

import io.awspring.cloud.s3.S3Exception;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;

@Component
public class AwsTemplate<T> {
    public T execute(AwsCallBack<T> action) {
        try {
            return action.execute();
        } catch (SdkClientException e) {
            throw new RuntimeException("클라이언트 측(서버) 문제로 인한 예외 발생", e);
        } catch (S3Exception e) {
            throw new RuntimeException("Amazon web service 중 S3에서 예외 발생", e);
        } catch (AwsServiceException e) {
            throw new RuntimeException("Amazon web service 에서 예외 발생", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException 발생", e);
        }
    }
}

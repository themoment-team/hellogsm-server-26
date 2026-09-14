package team.themoment.hellogsmv3.global.thirdParty.aws.sns.impl;

import static io.awspring.cloud.sns.sms.SmsType.*;

import org.springframework.stereotype.Service;

import io.awspring.cloud.sns.sms.SmsMessageAttributes;
import io.awspring.cloud.sns.sms.SnsSmsTemplate;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.thirdParty.aws.sns.SendSmsService;
import team.themoment.hellogsmv3.global.thirdParty.aws.sns.template.AwsTemplate;

@Service
@RequiredArgsConstructor
public class SendSmsServiceImpl implements SendSmsService {

    private static final String SENDER_ID = "hello-gsm";
    private static final String KR_CODE = "+82";
    private final SnsSmsTemplate smsTemplate;
    private final AwsTemplate<Void> executeWithExceptionHandle;

    @Override
    public void execute(String phoneNumber, String contentMessage, String footerMessage) {
        executeWithExceptionHandle.execute(() -> {
            smsTemplate.send(createPhoneNumber(phoneNumber),
                    contentMessage,
                    SmsMessageAttributes.builder().smsType(TRANSACTIONAL).senderID(SENDER_ID).build());

            smsTemplate.send(createPhoneNumber(phoneNumber),
                    footerMessage,
                    SmsMessageAttributes.builder().smsType(TRANSACTIONAL).senderID(SENDER_ID).build());
            return null;
        });
    }

    private static String createPhoneNumber(String phoneNumber) {
        return KR_CODE + phoneNumber;
    }
}

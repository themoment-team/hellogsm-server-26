package team.themoment.hellogsmv3.global.thirdParty.aws.sns;

public interface SendSmsService {
    void execute(String phoneNumber, String contentMessage, String footerMessage);
}

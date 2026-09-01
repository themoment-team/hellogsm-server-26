package team.themoment.hellogsmv3.domain.member.service;

public interface SendCodeNotificationService {
    void execute(String phoneNumber, String code);
}

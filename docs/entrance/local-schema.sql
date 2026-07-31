-- 로컬 개발용 스키마 스냅샷 — hellogsm-server-25 JPA 엔티티 기준 Hibernate 생성 DDL.
--
-- 어떻게 만들었나: `HIBERNATE_DDL_AUTO=create`로 `:server:bootRun`을 로컬 MySQL에 한 번 띄워
-- Hibernate가 실제로 만든 테이블을 `mysqldump --no-data`로 떠서, FK 의존순으로 정렬만 했다.
-- 손으로 옮겨 적은 추정 DDL이 아니라 Hibernate가 그 시점에 실제로 실행한 DDL 그대로다.
--
-- 주의: 이 파일은 스냅샷이다. 엔티티(persistence 모듈)에 컬럼/테이블 변경이 생기면 자동으로
-- 갱신되지 않는다 — 최신 상태가 필요하면 아래 절차로 다시 떠야 한다
-- (docs/test_guide.md 1.2 참고):
--   1. 로컬 MySQL을 깨끗한 상태로 준비
--   2. HIBERNATE_DDL_AUTO=create 로 :server:bootRun 1회 기동 후 종료
--   3. mysqldump --no-data --skip-comments --compact -u<user> <db> 로 떠서 이 파일을 갱신

CREATE TABLE `tb_member` (
  `birth` date DEFAULT NULL,
  `created_time` datetime(6) NOT NULL,
  `member_id` bigint NOT NULL AUTO_INCREMENT,
  `updated_time` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `auth_referrer_type` enum('GOOGLE','KAKAO') NOT NULL,
  `role` enum('ADMIN','APPLICANT','ROOT','UNAUTHENTICATED') DEFAULT NULL,
  `sex` enum('FEMALE','MALE') DEFAULT NULL,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `UK73mgps4yivl6ingxhhcqrt4sj` (`phone_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_oneseo` (
  `examination_number` varchar(4) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `oneseo_id` bigint NOT NULL AUTO_INCREMENT,
  `oneseo_submit_code` varchar(255) DEFAULT NULL,
  `applied_screening` enum('EXTRA_ADMISSION','EXTRA_VETERANS','GENERAL','SPECIAL') DEFAULT NULL,
  `decided_major` enum('AI','IOT','SW') DEFAULT NULL,
  `entrance_intention_yn` enum('NO','YES') DEFAULT NULL,
  `first_desired_major` enum('AI','IOT','SW') NOT NULL,
  `oneseo_edit_status` enum('APPROVED','NONE','REQUESTED') NOT NULL,
  `pass_yn` enum('NO','YES') DEFAULT NULL,
  `real_oneseo_arrived_yn` enum('NO','YES') NOT NULL,
  `second_desired_major` enum('AI','IOT','SW') NOT NULL,
  `third_desired_major` enum('AI','IOT','SW') NOT NULL,
  `wanted_screening` enum('EXTRA_ADMISSION','EXTRA_VETERANS','GENERAL','SPECIAL') NOT NULL,
  PRIMARY KEY (`oneseo_id`),
  UNIQUE KEY `UK1esr5cod9lmqlujsxfm0gmi5q` (`member_id`),
  UNIQUE KEY `UK1inc92d8xxr1mpp3ocxqct1pg` (`examination_number`),
  UNIQUE KEY `UK4yf1juupwyyjc74w0d3yft7wi` (`oneseo_submit_code`),
  KEY `idx_applied_screening_and_real_oneseo_arrived_yn` (`applied_screening`,`real_oneseo_arrived_yn`),
  KEY `idx_real_oneseo_arrived_yn` (`real_oneseo_arrived_yn`),
  CONSTRAINT `FK52tn2tcw8hscs5bx6hp0tdwle` FOREIGN KEY (`member_id`) REFERENCES `tb_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_entrance_test_factors_detail` (
  `arts_physical_subjects_score` decimal(6,3) DEFAULT NULL,
  `attendance_score` decimal(6,3) DEFAULT NULL,
  `general_subjects_score` decimal(6,3) DEFAULT NULL,
  `score_1_2` decimal(6,3) DEFAULT NULL,
  `score_2_1` decimal(6,3) DEFAULT NULL,
  `score_2_2` decimal(6,3) DEFAULT NULL,
  `score_3_1` decimal(6,3) DEFAULT NULL,
  `score_3_2` decimal(6,3) DEFAULT NULL,
  `total_non_subjects_score` decimal(6,3) DEFAULT NULL,
  `total_subjects_score` decimal(6,3) DEFAULT NULL,
  `volunteer_score` decimal(6,3) DEFAULT NULL,
  `entrance_test_factors_detail_id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`entrance_test_factors_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_entrance_test_result` (
  `competency_evaluation_score` decimal(38,2) DEFAULT NULL,
  `document_evaluation_score` decimal(6,3) DEFAULT NULL,
  `interview_score` decimal(38,2) DEFAULT NULL,
  `entrance_test_factors_detail_id` bigint NOT NULL,
  `entrance_test_result_id` bigint NOT NULL AUTO_INCREMENT,
  `oneseo_id` bigint NOT NULL,
  `first_test_pass_yn` enum('NO','YES') DEFAULT NULL,
  `second_test_pass_yn` enum('NO','YES') DEFAULT NULL,
  PRIMARY KEY (`entrance_test_result_id`),
  UNIQUE KEY `UKqv91eqs4fy6wl9p6tv0rc29tq` (`entrance_test_factors_detail_id`),
  UNIQUE KEY `UKjoa4uck9qn1qrso2uw93u3v9t` (`oneseo_id`),
  KEY `idx_first_test_pass_yn` (`first_test_pass_yn`),
  KEY `idx_second_test_pass_yn` (`second_test_pass_yn`),
  CONSTRAINT `FKe2ywx10leq6hpiwjyut4ownr1` FOREIGN KEY (`oneseo_id`) REFERENCES `tb_oneseo` (`oneseo_id`),
  CONSTRAINT `FKfesf149n6eso6kfdjbg2v7pnn` FOREIGN KEY (`entrance_test_factors_detail_id`) REFERENCES `tb_entrance_test_factors_detail` (`entrance_test_factors_detail_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_middle_school_achievement` (
  `ged_avg_score` decimal(5,2) DEFAULT NULL,
  `oneseo_id` bigint NOT NULL,
  `absent_days` varchar(255) DEFAULT NULL,
  `achievement_1_1` varchar(255) DEFAULT NULL,
  `achievement_1_2` varchar(255) DEFAULT NULL,
  `achievement_2_1` varchar(255) DEFAULT NULL,
  `achievement_2_2` varchar(255) DEFAULT NULL,
  `achievement_3_1` varchar(255) DEFAULT NULL,
  `achievement_3_2` varchar(255) DEFAULT NULL,
  `arts_physical_achievement` varchar(255) DEFAULT NULL,
  `arts_physical_subjects` varchar(255) DEFAULT NULL,
  `attendance_days` varchar(255) DEFAULT NULL,
  `free_semester` varchar(255) DEFAULT NULL,
  `general_subjects` varchar(255) DEFAULT NULL,
  `liberal_system` varchar(255) DEFAULT NULL,
  `new_subjects` varchar(255) DEFAULT NULL,
  `volunteer_time` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`oneseo_id`),
  CONSTRAINT `FKmqxodlarggs54ljfvnmavuafm` FOREIGN KEY (`oneseo_id`) REFERENCES `tb_oneseo` (`oneseo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_oneseo_privacy_detail` (
  `oneseo_id` bigint NOT NULL,
  `address` varchar(255) NOT NULL,
  `detail_address` varchar(255) NOT NULL,
  `graduation_date` char(7) NOT NULL,
  `guardian_name` varchar(255) NOT NULL,
  `guardian_phone_number` varchar(255) NOT NULL,
  `profile_img` varchar(255) NOT NULL,
  `relationship_with_guardian` varchar(255) NOT NULL,
  `school_address` varchar(255) DEFAULT NULL,
  `school_name` varchar(255) DEFAULT NULL,
  `school_teacher_name` varchar(255) DEFAULT NULL,
  `school_teacher_phone_number` varchar(255) DEFAULT NULL,
  `student_number` varchar(255) DEFAULT NULL,
  `graduation_type` enum('CANDIDATE','GED','GRADUATE') NOT NULL,
  PRIMARY KEY (`oneseo_id`),
  CONSTRAINT `FK9uatrs2lfgor5cxjhdsit7pmu` FOREIGN KEY (`oneseo_id`) REFERENCES `tb_oneseo` (`oneseo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_wanted_screening_change_history` (
  `created_time` datetime(6) NOT NULL,
  `entrance_test_result_id` bigint NOT NULL AUTO_INCREMENT,
  `oneseo_id` bigint NOT NULL,
  `after_screening` enum('EXTRA_ADMISSION','EXTRA_VETERANS','GENERAL','SPECIAL') NOT NULL,
  `before_screening` enum('EXTRA_ADMISSION','EXTRA_VETERANS','GENERAL','SPECIAL') NOT NULL,
  PRIMARY KEY (`entrance_test_result_id`),
  KEY `FKq94l0mankmnepq8frgurw2rff` (`oneseo_id`),
  CONSTRAINT `FKq94l0mankmnepq8frgurw2rff` FOREIGN KEY (`oneseo_id`) REFERENCES `tb_oneseo` (`oneseo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tb_operation_test_result` (
  `operarion_test_result_id` bigint NOT NULL,
  `first_test_result_announcement_yn` enum('NO','YES') NOT NULL DEFAULT 'NO',
  `second_test_result_announcement_yn` enum('NO','YES') NOT NULL DEFAULT 'NO',
  PRIMARY KEY (`operarion_test_result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

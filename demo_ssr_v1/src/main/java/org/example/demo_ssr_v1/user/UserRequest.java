package org.example.demo_ssr_v1.user;

import lombok.Data;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.springframework.web.multipart.MultipartFile;

public class UserRequest {

    @Data
    public static class LoginDTO {
        private String username;
        private String password;

        public void validate() {
            if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("사용자 명을 입력하세요");
            if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("비밀번호를 입력하세요");
        }
    } // end of inner class

    @Data
    public static class JoinDTO {
        private String username;
        private String password;
        private String email;

        // MultipartFile - Spring 에서 파일 업로드를 처리하기 위한 인터페이스
        // 우리 프로젝트에서는 선택 사항이라 회원 가입시 null 또는 empty 상태가 될 수 있음
        private MultipartFile profileImage;

        public void validate() {
            if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("사용자 명을 입력하세요");
            if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("비밀번호를 입력하세요");
            if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("이메일을 입력하세요");
            if (!email.contains("@")) throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다");
        }

        // JoinDTO 를 User 타입으로 변환시키는 기능
        public User toEntity(String profileImageName) {
            return User.builder()
                    .username(this.username)
                    .password(this.password)
                    .email(this.email)
                    // DB에 MultipartFile 타입을 저장할 수 없다 (파일 이름만 저장할 예정)
                    .profileImage(profileImageName)
                    .build();
        }
    } // end of inner class

    @Data
    public static class UpdateDTO {
        private String password;
        private MultipartFile profileImage; // form 에 name 속성 이름과 동일해야 함
        private String profileImageFilename; // 추후 user.update 메서드에서 사용 함
        // username은 제외: 변경 불가능한 고유 식별자

        public void validate() {
            if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("비밀번호를 입력하세요");
            if (password.length() < 4) throw new IllegalArgumentException("비밀번호는 8글자 이상이여야 합니다");

        }
    }

    @Data
    public static class EmailCheckDTO {
        private String email;
        private String code;
        // 추후 이메일 인증번호도 추가할 예정

        public void validate() {
            if (email == null || email.trim().isEmpty()) {
                // Exception400 추후 수정
                throw new Exception400("이메일을 입력해주세요");
            }
            if (!email.contains("@")) {
                throw new Exception400("올바른 이메일 형식이 아닙니다");
            }
        }
    }

    @Data
    public static class PointChargeDTO {
        private Integer amount;

        public void validate() {
            if (amount == null || amount <= 0) {
                throw new Exception400("층전할 포인트는 0보다 커야 합니다.");
            }
        }
    }
}

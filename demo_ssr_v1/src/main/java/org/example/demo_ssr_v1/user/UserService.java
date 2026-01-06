package org.example.demo_ssr_v1.user;

import lombok.RequiredArgsConstructor;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.errors.exception.Exception403;
import org.example.demo_ssr_v1._core.errors.exception.Exception404;
import org.example.demo_ssr_v1._core.errors.exception.Exception500;
import org.example.demo_ssr_v1._core.utils.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

// Service -> 응답 DTO 설계 해서 전달 -> Controller

/**
 * 사용자 서비스 레이어
 *
 * 1. 역할
 *  - 비즈니스 로직을 처리하는 계층
 *  - Controller 와 Repository 사시의 중간 계층
 *  - 트랜잭션 관리
 *  - 여러 Repository를 조합하여 복잡한 비즈니스 로직을 처리
 */
@Service // IoC 대상 @Component의 특수한 형태
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    // 객체 지향 개념 --> SOLID 원칙
    // DIP - 추상화가 높은 녀석을 선언하는 것이 좋다.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${tenco.key}")
    private String tencoKey;

    @Transactional
    public User 카카오소셜로그인(String code) {
        // 1. 인가 코드로 엑세스 토큰 발급
        UserResponse.OAuthToken oAuthToken = 카카오엑세스토큰발급(code);

        // 2. 엑세스 토큰으로 프로필 정보 조회
        UserResponse.KakaoProfile kakaoProfile = 카카오프로필조회(oAuthToken.getAccessToken());

        // 3. 프로필 정보로 사용자 생성 또는 조회
        User user = 카카오사용자생성또는조회(kakaoProfile);

        // 4. 로그인 처리 (엔티티 반환) -> 컨트롤러 단으로 users 반환
        return user;
    }

    /**
     * 카카오 인가 코드로 엑세스 토큰 발급
     * @param code 카카오 인가 코드
     * @return OAuth 엑세스 토큰 정보
     */
    private UserResponse.OAuthToken 카카오엑세스토큰발급(String code) {
        // 2. 토큰 발급 요청 (https://kauth.kakao.com/oauth/token) - POST
        // 2-1 HTTP 헤더 커스텀 -
        // Content-Type: application/x-www-form-urlencoded;charset=utf-8

        // 2-2
        RestTemplate restTemplate = new RestTemplate();

        // 2-3
        // HTTP 메시지 헤더 구성
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 2-4 HTTP 메시지 바디 구성( POST )
        MultiValueMap<String, String> tokenParams = new LinkedMultiValueMap<>();
        tokenParams.add("grant_type", "authorization_code");
        tokenParams.add("client_id", "2c1388b61d2bcc19c6643ded52612693");
        tokenParams.add("redirect_uri", "http://localhost:8080/user/kakao");
        tokenParams.add("code", code);

        // TODO - env 파일에 옮겨야 함 시크릿 추가(노출 금지)
        tokenParams.add("client_secret", "9Ej0hDYMTfhCQ2CVqv1VD7elHm0rfVd9");

        // 2-5 바디 + 헤더 구성
        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(tokenParams, tokenHeaders);

        // 2-6 요청하고 JWT 토큰 응답 받기 (카카오에서 엑세스 토큰이라고 부름)
        ResponseEntity<UserResponse.OAuthToken> tokenResponse = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                tokenRequest,
                UserResponse.OAuthToken.class
        );

        UserResponse.OAuthToken oAuthToken = tokenResponse.getBody();

        return oAuthToken;
    }

    /**
     * 카카오 엑세스 토큰으로 프로필 정보 조회
     * @param accessToken 카카오 엑세스 토큰
     * @return 카카오 프로필 정보
     */
    private UserResponse.KakaoProfile 카카오프로필조회(String accessToken) {
        // 3-1 HTTP 클라이언트 선언
        RestTemplate profileRt = new RestTemplate();

        // 3-2 HTTP 메시지 헤더 커스텀
        HttpHeaders profileHeaders = new HttpHeaders();
        // Bearer + 공백 한칸 무조건 (안하면 오류 발생)
        profileHeaders.add("Authorization", "Bearer " + accessToken);
        profileHeaders.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        // 3-3 요청 메시지 (요청 엔티티) 생성 -> 요청 바디 없음
        HttpEntity<Void> profileRequest = new HttpEntity<>(profileHeaders);

        ResponseEntity<UserResponse.KakaoProfile> profileResponse = profileRt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                profileRequest,
                UserResponse.KakaoProfile.class
        );

        UserResponse.KakaoProfile kakaoProfile = profileResponse.getBody();

        return kakaoProfile;
    }

    @Transactional
    public User 카카오사용자생성또는조회(UserResponse.KakaoProfile kakaoProfile) {
        String username = kakaoProfile.getProperties().getNickname() + "_" + kakaoProfile.getId();

        // 새로 생성한 username이 DB에 있다면 -> 이전에 회원가입 함 / 없을 시 -> 회원가입
        // 사용자 이름 조회 쿼리 수행
        // userOrigin 유저 || null
        User userOrigin = 사용자이름조회(username);
        if (userOrigin == null) {
            // 최초 카카오 소셜 로그인 사용자 임
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(tencoKey)) // 소셜 로그인은 임시 비밀번호로 설정 한다.
                    .email(username + "@kakao.com") // 선택 사항 (카카오 이메일 비즈니스 앱 신청)
                    .provider(OAuthProvider.KAKAO)
                    .build();

            String profileImage = kakaoProfile.getProperties().getProfileImage();
            if (profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage); // 카카오에서 넘겨 받은 URL 그대로 저장
            }

            소셜회원가입(newUser);
            // 조심 해야 함! 반드시 필요함
            userOrigin = newUser; // 반드시 넣어 줘야 함 -> Why? 로그인 처리 해야 함
        }
        return userOrigin;
    }

    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {

        // 1. 사용자명 중복 체크
        if(userRepository.findByUsername(joinDTO.getUsername()).isPresent()) {
            // isPresent -> 있으면 true 반환 , 없으면 false 반환
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        }

        // 1-1 이메일 중복 체크
        if (userRepository.findByEmail(joinDTO.getEmail()).isPresent()) {
            throw new Exception400("이미 등록된 이메일 입니다");
        }

        // User 엔티티에 저장할 때는 String 이어야 하고 null 값도 가질 수 있음
        String profileImageFileName = null;

        // 2. 회원 가입시 파일이 넘어 왔는 확인
        // TODO - 수정 해야 함.
        // MultipartFile (기본적으로 null이 넘어올 수 도 "" 공백으로 들어 올 수 있음)
        if(joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty() ) {

            // 2.1 유효성 검사 (이미지 파일 이어야 함)
            try {
                if(!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다");
                }
                profileImageFileName = FileUtil.saveFile(joinDTO.getProfileImage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        // 해싱 --> 해시값을 만들어 줌...
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword());

        User user = joinDTO.toEntity(profileImageFileName);
        // 비밀 번호를 평문에서 해시값으로 변경 해주어야 한다.
        user.setPassword(hashPwd);

        return userRepository.save(user);
    }

    // 로그인 처리
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        // 사용자가 던진 값과 DB에 있는 사용자 이름과 비밀번호를 확인 필요
        User userEntity = userRepository.findByUsernameWithRoles(loginDTO.getUsername())
                .orElseThrow(null); // 로그인 실패시 null 반환

        if (userEntity == null) {
            throw new Exception400("사용자가 존재하지 않습니다.");
        }

        // 비밀번호 검증 (BCrypt matches 메서드를 사용해서 비교 하면 된다)
        // 일치하면 true, 불일치 하면 false 반환
        if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다.");
        }

        // 기존 샘플 데이터로 회원가입한 사용자들로는 로그인을 못 함!!
        return userEntity;
    }

    // 회원 정보 수정
    public User 회원정보수정화면(Long userId) {

        User userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다"));

        if (!userEntity.isOwner(userId)) {
            throw new Exception403("회원 정보 수정 권한이 없습니다.");
        }

        return userEntity;
    }

    // 데이터의 수정 (더티 체킹 -> 반드시 먼저 조회 -> 조회된 객체의 상태값 변경 -> 자동 반영)
    // 1. 회원 정보 조회
    // 2. 인가 검사
    // 3. 엔티티 상태 변경 (더티 체킹)
    // 4. 트랜잭션이 일어나고 변경 된 User 엔티티 반환
    @Transactional
    public User 회원정보수정(UserRequest.UpdateDTO updateDTO, Long userId) {
        User userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다."));

        if (!userEntity.isOwner(userId)) {
            throw new Exception403("회원 정보 수정 권한이 없습니다.");
        }

        // 추가 - 프로필 이미지 처리
        // 중요: 우리 프로젝트 에서는 이미지 수정도 선택 사항 입니다.
        // 새로운 이미지 파일을 생성하고 기존에 있던 이미지 파일을 삭제해야 한다.
        // 추가로 DB 정보도 업데이트 해야한다.
        String oldProfileImage = userEntity.getProfileImage();
        // 분기 처리 - 이미지명이 있거나 또는 null 값이다.
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            // 1. 이미지 파일인지 검증
            if (!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                throw new Exception400("이미지 파일만 업로드 가능합니다.");
            }

            // 2. 새 이미지 저장
            try {
                String newProfileImageFilename = FileUtil.saveFile(updateDTO.getProfileImage());
                // 새로 만들어진 파일 이름을 잠시 DTO에 보관 함
                updateDTO.setProfileImageFilename(newProfileImageFilename);

                if (oldProfileImage != null && !oldProfileImage.isEmpty()) {
                    // 내가 기존에 있던 이미지를 삭제 처리 한다.
                    FileUtil.deleteFile(oldProfileImage);
                }
            } catch (IOException e) {
                throw new Exception500("파일 저장에 실패했습니다.");
            }
            // end of 파일이 들어 왔을 때 처리
        } else {
            // 새 이미지가 업로드 되지 않았으면 기존 이미지 파일 이름 유지
            updateDTO.setProfileImageFilename(oldProfileImage);
        }
        // 비밀번호 암호화 처리
        String hashPsw = passwordEncoder.encode(updateDTO.getPassword());
        updateDTO.setPassword(hashPsw);

        // 객체 상태값 변경 (트랜잭션이 끝나면 자동으로 commit 및 반영)
        userEntity.update(updateDTO);
        return userEntity;
    }

    public User 마이페이지(Long sessionUserId) {
        User user = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new Exception404("해당 유저를 찾을 수 없습니다."));

        // 인가 처리
        if (!user.isOwner(sessionUserId)) {
            throw new Exception403("권한이 없습니다.");
        }

        return user;
    }

    @Transactional
    public User 프로필이미지삭제(Long sessionUserId) {
        // 1. 회원 정보 조회
        // 2. 회원 정보와 세션 id 값이 같은지 판단 -> 인가 처리
        // 3. 프로필 이미지가 있다면 삭제 (FileUtil) 헬퍼 클래스 사용할 예정 (디스크에서 삭제)
        // 4. DB 에서 프로필 이름 null 로 업데이트 처리
        User userEntity = userRepository.findById(sessionUserId)
                .orElseThrow(() -> new Exception404("해당 유저는 존재하지 않습니다."));

        if (!userEntity.isOwner(sessionUserId)) {
            throw new Exception403("프로필 이미지 삭제 권한이 없습니다.");
        }

        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                FileUtil.deleteFile(profileImage);
            } catch (IOException e) {
                System.err.println("프로필 이미지 파일 삭제 실패");
            }
        }

        // 객세 상태값 변경 (트랜 잭션이 끝나는 시점 더티 체킹 됨)
        userEntity.setProfileImage(null);

        return userEntity;
    }

    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public void 소셜회원가입(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User 포인트충전(Long userId, Integer amount) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("해당 유저를 찾을 수 없습니다."));
        // 2. 포인트 충전
        user.chargePoint(amount);
        // 3. 변경된 엔티티 반환
        return userRepository.save(user);
    }
}

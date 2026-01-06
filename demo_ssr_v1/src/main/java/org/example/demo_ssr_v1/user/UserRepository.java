package org.example.demo_ssr_v1.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JpaRepository -> @Repository 있음 -> IoC 됨
public interface UserRepository extends JpaRepository<User, Long> {

    // 쿼리 메서드 네이밍 (자동 쿼리 생성)
    Optional<User> findByUsername(String username);
//    Optional<User> findByUsernameAndPassword(String username, String password);

    /**
     * 로그인시 역할(ROLE) 정보까지 함께 조회되는 메서드
     * - 세션에 저장된 User 객체에서 isAdmin(), getRoleDisplay() 등을 바로 사용할 수 있다.
     */
//    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r " +
//            "WHERE u.username = :username AND u.password = :password ")
//    Optional<User> findByUsernameAndPasswordWithRoles(
//            @Param("username") String username,
//            @Param("password") String password
//    );

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r " +
            "WHERE u.username = :username ")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    // 이메일 존재 여부 확인 쿼리 메서드 설계
    Optional<User> findByEmail(String email);
}

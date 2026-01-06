package org.example.demo_ssr_v1._core.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.demo_ssr_v1._core.errors.exception.Exception401;
import org.example.demo_ssr_v1.user.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 로그인 인증 확인용 인터셉터 구축
 *
 * 컨트롤러에 진입하기 전에 세션에 로그인 정보가 있는지 없는지 확인
 * 로그인 하지 않은 사용자는 E..401 처리 예정
 */
// 1. HandlerInterceptor 구현
@Component // IoC 컨테이너에 이 클래스를 빈 객체로 등록 함 (싱글톤 패턴으로 관리 됨)
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * preHandle: 컨트롤러 진입 전에 실행되는 메서드
     * 동작 흐름 - 요청이 들어오면 (등록가 URI 시) 컨트롤러 보다 먼저 동작합니다.
     *
     * @param request current HTTP 요청 객체
     * @param response current HTTP 응답 객체
     * @param handler 실행 될 핸들러(컨트롤러 메서드)
     * @return true (컨트롤러로 진입) / false (진입 X)
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 요청 객체에서 사용자 정보인 세션 정보가 있는지 없는지 확인
        HttpSession session = request.getSession();
        User sessionUser = (User) session.getAttribute("sessionUser");
        if (sessionUser == null) {
            throw new Exception401("로그인을 먼저 진행해야합니다.");
        }

        // 로그인 정보가 있으면 컨트롤러 단으로 진입 허용

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    /**
     * postHandle: 컨트롤러 실행 후,뷰 렌더링 전에 실행되는 메서드
     *
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    /**
     * afterCompletion: 요청 처리가 완전히 끝난 후 실행되는 메서드
     *
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}

package org.example.demo_ssr_v1.payment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1._core.utils.MyDateUtil;

import java.sql.Timestamp;

public class PaymentResponse {

    @Data
    public static class PrepareDTO {
        private String merchantUid; // 생성된 우리 서버 주문 번호
        private Integer amount; // 결재 금액
        private String impKey; // 포트원 REST API 키 (필수)

        public PrepareDTO(String merchantUid, Integer amount, String impKey) {
            this.merchantUid = merchantUid;
            this.amount = amount;
            this.impKey = impKey;
        }
    }

    // 결제 검증 응답 DTO - JS로 내려줄 데이터
    @Data
    public static class VerifyDTO {
        private Integer amount;
        private Integer currentPoint;

        public VerifyDTO(Integer amount, Integer currentPoint) {
            this.amount = amount;
            this.currentPoint = currentPoint;
        }
    }

    // 포트원 엑세스 토큰 응답 DTO 설계
    @Data
    public static class PortOneTokenResponse {
        private int code;
        private String message;
        private ResponseData response;

        // 중첩 객체를 설계 해야 함
        @Data
        @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class ResponseData {
            // access_token -> @JsonNaming -> accessToken
            private String accessToken;
            private int now;
            private int expiredAt;
        }
    }

    // 포트원 결제(포트원 서버에 DB 저장되어 있음) 조회 응답 DTO
    @Data
    public static class PortOnePaymentResponse {
        private int code;
        private String message;
        private PaymentData response;

        @Data
        @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class PaymentData {
            private Integer amount;
            private String impUid;
            private String merchantUid;
            private String status;
            private Long paidAt;
        }
    }

    @Data
    public static class ListDTO {
        private Long id;
        private String merchantUid;
        private String impUid;
        private String timestamp;
        private Integer amount;
        private String status;
        private String statusDisplay;

        // 환불 관련 추가
        private Boolean isRefundable; // 환불 가능 여부 (화면에 표시 여부)

        public ListDTO(Payment payment, Boolean isRefundable) {
            this.id = payment.getId();
            this.merchantUid = payment.getMerchantUid();
            this.impUid = payment.getImpUid();
            this.isRefundable = isRefundable != null ? isRefundable : false;
            if (payment.getTimestamp() != null) {
                this.timestamp = MyDateUtil.time(payment.getTimestamp());
            }
            this.amount = payment.getAmount();
            if ("paid".equals(payment.getStatus())) {
                this.statusDisplay = "결제완료";
            } else {
                this.statusDisplay = "환불완료";
            }
        }
    }
}

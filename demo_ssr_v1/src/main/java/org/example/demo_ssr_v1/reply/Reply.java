package org.example.demo_ssr_v1.reply;

import jakarta.persistence.*;
import lombok.*;
import org.example.demo_ssr_v1._core.errors.exception.Exception400;
import org.example.demo_ssr_v1.board.Board;
import org.example.demo_ssr_v1.user.User;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@Table(name = "reply_tb")
@Entity
public class Reply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @CreationTimestamp
    private Timestamp createdAt;

    @Builder
    public Reply(String comment, User user, Board board) {
        this.comment = comment;
        this.user = user;
        this.board = board;
    }

    // 소유자 여부 (댓글)
    public boolean isOwner(Long userid) {
        if (this.user == null || userid == null) {
            return false;
        }

        Long replyUserId = this.user.getId();
        if (replyUserId == null) {
            return false;
        }

        boolean result = (replyUserId.equals(userid));
        return result;
    }

    // 댓글 내용 수정
    public void update(String newString) {
        if (newString == null || newString.trim().isEmpty()) {
            throw new Exception400("댓글 내용은 필수 입니다.");
        }

        if (newString.length() > 500) {
            throw new Exception400("댓글은 500자 이하여야 합니다.");
        }


    }
}

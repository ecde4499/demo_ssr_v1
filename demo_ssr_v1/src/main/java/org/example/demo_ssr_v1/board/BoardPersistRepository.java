package org.example.demo_ssr_v1.board;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// DB -- CRUD
@Repository
@RequiredArgsConstructor
public class BoardPersistRepository {

    // DI
    private final EntityManager entityManager;

    @Transactional
    public Board save(Board board) {
        // 엔티티 매니저가 자동으로 insert 쿼리를 만들어서 던진다
        entityManager.persist(board);

        return board;
    }

    // 게시글 전체 조회
    public List<Board> findAll() {
        return entityManager
                .createQuery("SELECT b FROM Board b ORDER BY b.createdAt DESC")
                .getResultList();
    }

    // 게시글 단건 조회
    public Board findById(Long id) {
        Board board = entityManager.find(Board.class, id);

        return board;
    }

    // 게시글 수정
    @Transactional
    public Board updateById(Long id, BoardRequest.UpdateDTO reqDTO) {
        Board board = entityManager.find(Board.class, id);

        if (board == null) {
            throw new IllegalArgumentException("수정할 게시글을 찾을 수 없습니다.");
        }

//        board.setTitle(req.getTitle());
//        board.setContent(req.getContent());
//        board.setUsername(req.getUsername());
        board.update(reqDTO);

        // 더티 체킹
        // 1. 개발자가 직접 update 쿼리를 작성 안해도 된다.
        // 2. 변경된 필드만 자동으로 update 된다
        // 3. 영속성 컨텍스트가 엔티티 상태를 자동 관리 한다.
        // 4. 1차 캐시의 엔티티 정보도 자동 갱신

        return board;
    }

    // 게시글 삭제
    @Transactional
    public void deleteById(Long id) {
        Board board = entityManager.find(Board.class, id);

        if (board == null) {
            throw new IllegalArgumentException("삭제할 게시글을 찾을 수 없습니다.");
        }

        entityManager.remove(board);
    }

}

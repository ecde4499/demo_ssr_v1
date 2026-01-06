package org.example.demo_ssr_v1._api;

import lombok.Builder;
import lombok.Data;

@Data
// 요청할 때 보내줘야하는 데이터 RequestDTO
public class Post {
    private Integer id;
    private String title;
    private String body;
    private Integer userId;

    @Builder
    public Post(Integer id, String title, String body, Integer userId) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.userId = userId;
    }
}

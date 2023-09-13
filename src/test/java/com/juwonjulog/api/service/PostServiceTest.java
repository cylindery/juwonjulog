package com.juwonjulog.api.service;

import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.repository.PostRepository;
import com.juwonjulog.api.request.PostCreate;
import com.juwonjulog.api.response.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.DESC;

@SpringBootTest
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void clean() {
        postRepository.deleteAll();
    }

    @Test
    @DisplayName("글 작성 요청 시 DB에 Post 데이터 저장")
    void save_post_to_db_when_write() {
        // given
        PostCreate postCreate = PostCreate.builder()
                .title("글 제목")
                .content("글 내용...")
                .build();

        // when
        postService.write(postCreate);

        // then
        assertEquals(1L, postRepository.count());
        Post post = postRepository.findAll().get(0);
        assertEquals("글 제목", post.getTitle());
        assertEquals("글 내용...", post.getContent());
    }
    
    @Test
    @DisplayName("DB에 존재하지 않는 글 1개 조회 시 예외 출력")
    void get_not_exist_post() {
        // given
        Long postId = 1L;
        
        // expected
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> postService.get(postId));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("DB에 저장된 글 1개 조회")
    void get_post_saved_in_db() {
        // given
        Post savedPost = Post.builder()
                .title("글 제목")
                .content("글 내용...")
                .build();
        postRepository.save(savedPost);

        // when
        PostResponse response = postService.get(savedPost.getId());

        // then
        assertNotNull(response);
        assertEquals("글 제목", response.getTitle());
        assertEquals("글 내용...", response.getContent());
    }

    @Test
    @DisplayName("DB에 저장된 1페이지 글 조회")
    void get_1_page_posts_saved_in_db() {
        // given
        List<Post> requestPosts = IntStream.range(1, 31)
                .mapToObj(i -> Post.builder()
                        .title("title_" + i)
                        .content("content_" + i)
                        .build())
                .collect(Collectors.toList());
        postRepository.saveAll(requestPosts);

        Pageable pageable = PageRequest.of(0, 5, DESC, "id");

        // when
        List<PostResponse> posts = postService.getList(pageable);

        // then
        assertEquals(5L, posts.size());
        assertEquals(30, posts.get(0).getId());
        assertEquals("title_30", posts.get(0).getTitle());
        assertEquals("content_30", posts.get(0).getContent());
        assertEquals(26, posts.get(4).getId());
        assertEquals("title_26", posts.get(4).getTitle());
        assertEquals("content_26", posts.get(4).getContent());
    }
}
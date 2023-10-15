package com.juwonjulog.api.service;

import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.exception.PostNotFound;
import com.juwonjulog.api.repository.PostRepository;
import com.juwonjulog.api.request.PostCreate;
import com.juwonjulog.api.request.PostEdit;
import com.juwonjulog.api.request.PostSearch;
import com.juwonjulog.api.response.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("게시글 작성 시 DB에 Post 데이터 저장")
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
    @DisplayName("DB에 저장된 게시글 단건 조회")
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
    @DisplayName("DB에 존재하지 않는 게시글 단건 조회 시 예외 출력")
    void get_not_exists_post() {
        // expected
        Throwable exception = assertThrows(PostNotFound.class, () -> postService.get(1L));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("DB에 저장된 게시글 1페이지 10개 조회")
    void get_1_page_10_posts_saved_in_db() {
        // given
        List<Post> requestPosts = IntStream.range(1, 31)
                .mapToObj(i -> Post.builder()
                        .title("title_" + i)
                        .content("content_" + i)
                        .build())
                .collect(Collectors.toList());
        postRepository.saveAll(requestPosts);

        PostSearch postSearch = PostSearch.builder()
                .page(1)
                .size(10)
                .build();

        // when
        List<PostResponse> posts = postService.getList(postSearch);

        // then
        assertEquals(10L, posts.size());
        assertEquals("title_30", posts.get(0).getTitle());
        assertEquals("content_30", posts.get(0).getContent());
        assertEquals("title_21", posts.get(9).getTitle());
        assertEquals("content_21", posts.get(9).getContent());
    }

    @Test
    @DisplayName("게시글 제목 수정")
    void edit_post_title() {
        // given
        Post post = Post.builder()
                .title("title")
                .content("content")
                .build();
        postRepository.save(post);

        PostEdit postEdit = PostEdit.builder()
                .title("edited_title")
                .content("content")
                .build();

        // when
        postService.edit(post.getId(), postEdit);

        // then
        Post editedPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 글입니다. id=" + post.getId()));
        assertEquals("edited_title", editedPost.getTitle());
        assertEquals("content", editedPost.getContent());
    }

    @Test
    @DisplayName("게시글 내용 수정")
    void edit_post_content() {
        // given
        Post post = Post.builder()
                .title("title")
                .content("content")
                .build();
        postRepository.save(post);

        PostEdit postEdit = PostEdit.builder()
                .title("title")
                .content("edited_content")
                .build();

        // when
        postService.edit(post.getId(), postEdit);

        // then
        Post editedPost = postRepository.findById(post.getId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 글입니다. id=" + post.getId()));
        assertEquals("title", editedPost.getTitle());
        assertEquals("edited_content", editedPost.getContent());
    }

    @Test
    @DisplayName("DB에 존재하지 않는 게시글 수정 시 예외 출력")
    void edit_not_exists_post() {
        // given
        PostEdit postEdit = PostEdit.builder()
                .title("edited_title")
                .content("edited_content")
                .build();

        // expected
        Throwable exception = assertThrows(PostNotFound.class, () -> postService.edit(1L, postEdit));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
    }

    @Test
    @DisplayName("게시글 삭제")
    void delete_post() {
        // given
        Post post = Post.builder()
                .title("title")
                .content("content")
                .build();
        postRepository.save(post);

        // when
        postService.delete(post.getId());

        // then
        assertEquals(0, postRepository.count());
    }

    @Test
    @DisplayName("DB에 존재하지 않는 게시글 삭제 시 예외 출력")
    void delete_not_exists_post() {
        // expected
        Throwable exception = assertThrows(PostNotFound.class, () -> postService.delete(1L));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
    }
}
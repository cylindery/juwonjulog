package com.juwonjulog.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.repository.PostRepository;
import com.juwonjulog.api.request.PostCreate;
import com.juwonjulog.api.request.PostEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void clean() {
        postRepository.deleteAll();
    }

    @Test
    @DisplayName("게시글 작성 시 빈 오브젝트를 출력")
    void return_empty_object_when_post() throws Exception {
        // given
        PostCreate request = PostCreate.builder()
                .title("글 제목")
                .content("글 내용...")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/posts")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 작성 시 DB에 Post 데이터 저장")
    void save_post_to_db_when_post() throws Exception {
        // given
        PostCreate request = PostCreate.builder()
                .title("글 제목")
                .content("글 내용...")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // when
        mockMvc.perform(post("/posts")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andDo(print());

        // then
        assertEquals(1L, postRepository.count());

        Post post = postRepository.findAll().get(0);
        assertEquals("글 제목", post.getTitle());
        assertEquals("글 내용...", post.getContent());
    }

    @Test
    @DisplayName("게시글 작성 시 title 값이 null 또는 공백이면, json 에러 객체를 출력")
    void return_json_error_when_post_title_null_or_blank() throws Exception {
        // given
        PostCreate request = PostCreate.builder()
                .content("글 내용...")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/posts")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.title").value("제목을 입력해주세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 작성 시 제목에 욕 제한")
    void restrict_swear_word_in_title_when_post() throws Exception {
        // given
        PostCreate request = PostCreate.builder()
                .title("title_욕")
                .content("content")
                .build();

        String json = objectMapper.writeValueAsString(request);

        // expected
        mockMvc.perform(post("/posts")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.title").value("제목에 욕을 포함할 수 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 단건 조회")
    void get_post() throws Exception {
        // given
        Post post = Post.builder()
                .title("글 제목")
                .content("글 내용...")
                .build();
        postRepository.save(post);

        // expected
        mockMvc.perform(get("/posts/{postId}", post.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.title").value("글 제목"))
                .andExpect(jsonPath("$.content").value("글 내용..."))
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 단건 조회")
    void get_nonexistent_post() throws Exception {
        // expected
        mockMvc.perform(get("/posts/{postId}", 1L)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 단건 조회 시 title 길이는 최대 10")
    void title_max_length_is_10_when_get() throws Exception {
        // given
        Post postLong = Post.builder()
                .title("123456789012345")
                .content("글 내용...")
                .build();
        postRepository.save(postLong);

        Post postShort = Post.builder()
                .title("12345")
                .content("글 내용...")
                .build();
        postRepository.save(postShort);

        // expected
        mockMvc.perform(get("/posts/{postId}", postLong.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postLong.getId()))
                .andExpect(jsonPath("$.title").value("1234567890"))
                .andDo(print());

        mockMvc.perform(get("/posts/{postId}", postShort.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postShort.getId()))
                .andExpect(jsonPath("$.title").value("12345"))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 여러개 조회 시 1페이지, 글 10개 내림차순")
    void get_1_page_10_posts_desc_when_getList() throws Exception {
        // given
        List<Post> requestPosts = IntStream.range(1, 31)
                .mapToObj(i -> Post.builder()
                        .title("title_" + i)
                        .content("content_" + i)
                        .build())
                .collect(Collectors.toList());
        postRepository.saveAll(requestPosts);

        // expected
        mockMvc.perform(get("/posts?page=1&size=10")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(10)))
                .andExpect(jsonPath("$[0].title").value("title_30"))
                .andExpect(jsonPath("$[0].content").value("content_30"))
                .andExpect(jsonPath("$[9].title").value("title_21"))
                .andExpect(jsonPath("$[9].content").value("content_21"))
                .andDo(print());
    }

    @Test
    @DisplayName("0페이지 조회 요청해도 1페이지 출력")
    void get_1_page_when_get_0_page() throws Exception {
        // given
        List<Post> requestPosts = IntStream.range(1, 31)
                .mapToObj(i -> Post.builder()
                        .title("title_" + i)
                        .content("content_" + i)
                        .build())
                .collect(Collectors.toList());
        postRepository.saveAll(requestPosts);

        // expected
        mockMvc.perform(get("/posts?page=0&size=10")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(10)))
                .andExpect(jsonPath("$[0].title").value("title_30"))
                .andExpect(jsonPath("$[0].content").value("content_30"))
                .andExpect(jsonPath("$[9].title").value("title_21"))
                .andExpect(jsonPath("$[9].content").value("content_21"))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 제목 수정")
    void edit_post_title() throws Exception {
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

        String json = objectMapper.writeValueAsString(postEdit);

        // expected
        mockMvc.perform(patch("/posts/{postId}", post.getId())
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 내용 수정")
    void edit_post_content() throws Exception {
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

        String json = objectMapper.writeValueAsString(postEdit);

        // expected
        mockMvc.perform(patch("/posts/{postId}", post.getId())
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정")
    void edit_nonexistent_post() throws Exception {
        // given
        PostEdit postEdit = PostEdit.builder()
                .title("edited_title")
                .content("edited_content")
                .build();

        String json = objectMapper.writeValueAsString(postEdit);

        // expected
        mockMvc.perform(patch("/posts/{postId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 삭제")
    void delete_post() throws Exception {
        // given
        Post post = Post.builder()
                .title("title")
                .content("content")
                .build();
        postRepository.save(post);

        // expected
        mockMvc.perform(delete("/posts/{postId}", post.getId())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제")
    void delete_nonexistent_post() throws Exception {
        // expected
        mockMvc.perform(delete("/posts/{postId}", 1L)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
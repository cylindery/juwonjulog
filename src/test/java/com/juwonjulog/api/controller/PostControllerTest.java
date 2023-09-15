package com.juwonjulog.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.repository.PostRepository;
import com.juwonjulog.api.request.PostCreate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @DisplayName("/posts에 post 요청 시 빈 오브젝트를 출력한다.")
    void basic_post_request() throws Exception {
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
    @DisplayName("/posts에 post 요청 시 title 값이 null 또는 공백이면, json 에러 객체를 출력한다.")
    void title_error_when_post_request() throws Exception {
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
                .andExpect(jsonPath("$.validation.title").value("타이틀을 입력해주세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("/posts에 post 요청 시 DB에 Post 1개 저장")
    void save_post_to_db_when_post_request() throws Exception {
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
    @DisplayName("/posts/{postId}에 get 요청 시 글 1개 조회")
    void get_1_post() throws Exception {
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
    @DisplayName("/posts/{postId}에 get 요청 시 title 길이는 최대 10")
    void title_max_length_is_10() throws Exception {
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
    @DisplayName("/posts에 1페이지 글 10개 내림차순 조회 요청")
    void get_1_page_10_posts_desc() throws Exception {
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
    @DisplayName("/posts에 0페이지 조회 요청해도, 1페이지 출력")
    void test() throws Exception {
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
}
# juwonjulog

## 프로젝트 생성

### 프로젝트 환경

- Language: Java
- Type: Gradle - Groovy
- Project SDK: 11
- Java: 11
- Packaging: Jar
- Spring Boot: 2.7.15
- Dependencies: Lombok / Spring Web / Spring Data JPA / H2 Database

> 이후에 필요한 Dependency가 생기면 그때마다 추가.

### Spring Boot 실행, GitHub 연결

- src/main/resources/application.properties 파일을 **application.yml** 파일로 수정
- **.gitignore** 설정
- build.gradle 파일에서 dependencies 제대로 설치됐는지 확인
- **JuwonjuBlogApplication.java** 실행해서 스프링 띄워보기
    - Tomcat started on port(s): 8080 (http) with context path '' 로그 확인
    - 웹 브라우저에서 http://localhost:8080/ 접속. 404 에러 확인

## POST 컨트롤러 생성

기본적인 컨트롤러를 사용하기보다는, JSON 형태로 데이터 응답처리를 하는 api를 만드는 것이 목표.  
일단 블로그의 기본적인 글 페이지를 만들어, HTTP 메서드 GET을 호출해보자.

- GET 메서드를 사용하여 간단히 "Hello World" String을 리턴하도록 라우팅

```java

@RestController
public class PostController {

    @GetMapping("/posts")
    public String get() {
        return "Hello World";
    }

}
```

- 테스트 케이스 작성. 확인

```java

@WebMvcTest
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/posts에 GET 요청 시 Hello World를 출력한다.")
    void get() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"))
                .andDo(print());
    }

}
```

### 글 등록 기능 추가

블로그 사용자가 글을 등록하기 위해선 글 제목과 글 내용 데이터를 서버에 전달해야 한다.  
이러한 글 제목(title)과 글 내용(content) 데이터를 JSON 타입으로 HttpRequest에 담아 보내는 것이 목표.  
그리고 서버에서 데이터를 파라미터로 받기 위해, 따로 DTO 클래스를 만들자.

- DTO PostCreate 클래스

```java
package com.juwonjulog.api.controller.request;

public class PostCreate {

    public String title;
    public String content;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "PostCreate{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

}
```

- 이전에 간단히 GET Method로 라우팅했던 기능을 POST Method로 수정. 파라미터로 가져온 params를 로그 출력

```java

@Slf4j
@RestController
public class PostController {

    // 글 등록
    // POST Method
    @PostMapping("/posts")
    public String post(@RequestBody PostCreate params) {
        log.info("params={}", params);
        return "Hello World";
    }

}
```

- 테스트 케이스 작성. HttpRequest Body에 JSON 데이터({"title": "글 제목", "content": "글 내용..."}) 담아 보내기

```java

@WebMvcTest
class PostControllerTest {

    @Test
    @DisplayName("/posts에 POST 요청 시 Hello World를 출력한다.")
    void post() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"글 제목\", \"content\": \"글 내용...\"}")
                )
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World"))
                .andDo(print());
    }

}
```

## 데이터 검증

앞서 컨트롤러로 넘기는 json 데이터가 값이 누락되거나, 모종의 이유로 db에 저장될 때 오류가 발생할 수 있다.  
그러므로 컨트롤러로 넘기기 전 단계에서 미리 데이터 검증 단계를 추가하면, db에 저장할 때 프로세스의 안정감을 줄 수 있다.  
그리고 데이터 검증에서 에러가 발생했다면 어떤 에러인지 알 수 있게 json 타입으로 담아, Response Body에 넣어보자.

- Spring Boot Validation 라이브러리의 @Blank를 이용해 title 값이 null 또는 공백 데이터 체크

```java

@Getter
@Setter
@ToString
public class PostCreate {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

}
```

Spring Boot 버전이 높아짐에 따라, Spring Boot Validation을 dependencies에 추가해야 한다.

```
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

- @Valid로 검증. 에러 발생 시, BindingResult로 가져온 에러 내용을 json 데이터로 에러 메시지 넘기기

```java

@Slf4j
@RestController
public class PostController {

    // 글 등록
    // POST Method
    @PostMapping("/posts")
    public Map<String, String> post(@RequestBody @Valid PostCreate params, BindingResult result) {
        if (result.hasErrors()) {
            List<FieldError> fieldErrors = result.getFieldErrors();
            FieldError firstFieldError = fieldErrors.get(0);
            String fieldName = firstFieldError.getField(); // 타이틀
            String errorMessage = firstFieldError.getDefaultMessage(); // 에러 메시지

            Map<String, String> error = new HashMap<>();
            error.put(fieldName, errorMessage);
            return error;
        }

        return Map.of();
    }

}
```

- 테스트 케이스. jsonPath 표현식 사용

```java

@WebMvcTest
class PostControllerTest {

    @Test
    @DisplayName("/posts에 POST 요청 시 title 값이 null 또는 공백이면, json 에러 데이터를 출력해야 한다.")
    void postTest2() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": null, \"content\": \"글 내용...\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("타이틀을 입력해주세요."))
                .andDo(print());
    }

}
```

### 응답 클래스와 @ControllerAdvice를 통한 데이터 검증

현재 컨트롤러에 적용된 데이터 검증은 새로운 메서드가 추가될 때마다 매번 적용해줘야 하고, 또 동시에 여러개의 에러가 터졌을 때 대처하기도 힘들다.  
게다가 응답값을 HashMap을 사용하고 있는데 이보다는 따로 응답 클래스를 만들어주면서, @ControllerAdvice를 통해 예외처리를 분리/통합 해보자.

- 예외 응답 클래스 ErroResponse. json 형태로 에러에 대한 정보를 담는다
    - code: 예외 코드
    - message: 예외 메시지
    - validation: 구체적으로 어떤 필드가 잘못됐는지 설명

```java
package com.juwonjulog.api.response;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Map<String, String> validation = new HashMap<>();

    public void addValidation(String fieldName, String errorMessage) {
        this.validation.put(fieldName, errorMessage);
    }
}
```

- 모든 컨트롤러 전역에서 발생할 수 있는 예외를 처리할 수 있는 ExceptionController 클래스

```java
package com.juwonjulog.api.controller;

@Slf4j
@ControllerAdvice
public class ExceptionController {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ErrorResponse invalidRequestHandler(MethodArgumentNotValidException e) {
        ErrorResponse response = new ErrorResponse("400", "잘못된 요청입니다.");

        for (FieldError fieldError : e.getFieldErrors()) {
            response.addValidation(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return response;
    }

}
```

현재 title에 null 값이 들어올 시 발생하는 MethodArgumentNotValidException을 @ExceptionHandler을 통해 캐치한다.  
그리고 이렇게 잡히는 에러에 대해 @ResponseStatus로 Http 400 에러를 리턴하고, @ResponseBody로 에러 정보 HttpResponse Body에 삽입

- ExceptionContoller로 예외를 처리할 수 있도록 PostController 수정. 이전의 BindingResult 삭제

```java

@Slf4j
@RestController
public class PostController {

    @PostMapping("/posts")
    public Map<String, String> post(@RequestBody @Valid PostCreate params) {
        return Map.of();
    }

}
```

- 테스트 케이스

```java

@WebMvcTest
class PostControllerTest {

    @Test
    @DisplayName("/posts에 POST 요청 시 title 값이 null 또는 공백이면, json 에러 객체를 출력한다.")
    void postTest2() throws Exception {
        // expected
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": null, \"content\": \"글 내용...\"}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.validation.title").value("타이틀을 입력해주세요."))
                .andDo(print());
    }

}
```

## 작성글 저장

### 게시글 저장 구현

본격적으로 게시글을 저장하는 기능을 지원하는 서비스, 그리고 게시글이 저장되는 데이터베이스까지 구현해보자.  
'POST 컨트롤러 -> 게시글 저장 서비스 호출 -> 레포지토리 호출' 구조를 가질 것.

- Post 엔티티

```java
package com.juwonjulog.api.domain;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content;

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

}

```

엔티티의 id 생성 전략은 Identity. 그리고 게시글을 DB에 저장할 때, 글 내용은 큰 데이터를 저장할 수 있도록 @Lob 설정.

- PostRepository. 레포지토리

```java
package com.juwonjulog.api.repository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
```

- PostService. 서비스

```java
package com.juwonjulog.api.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public void write(PostCreate postCreate) {
        Post post = new Post(postCreate.getTitle(), postCreate.getContent());
        postRepository.save(post);
    }

}
```

컨트롤러에서 글을 저장할 때 PostService의 write()를 호출하면, 넘겨받은 PostCreate를 Post 엔티티로 변환해서 주입받은 레포지토리에 저장한다.

- PostController

```java
package com.juwonjulog.api.controller;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public Map<String, String> post(@RequestBody @Valid PostCreate request) {
        postService.write(request);
        return Map.of();
    }

}

```

서비스를 주입받아 게시글 저장 서비스 호출.

- 컨트롤러 테스트 케이스

```java

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void clean() {
        postRepository.deleteAll();
    }

    @Test
    @DisplayName("/posts에 POST 요청 시 DB에 Post 1개 저장")
    void postTest3() throws Exception {
        // when
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"글 제목\", \"content\": \"글 내용...\"}")
                )
                .andExpect(status().isOk())
                .andDo(print());

        // then
        assertEquals(1L, postRepository.count());

        Post post = postRepository.findAll().get(0);
        assertEquals("글 제목", post.getTitle());
        assertEquals("글 내용...", post.getContent());
    }

}
```

컨트롤러 - 서비스 - 레포지토리에 이르는 api 전반적 테스트를 하기 위해, @WebMvcTest에서 @SpringBootTest 수정. 그런데 @SpringBootTest 애노테이션만으로는 MockMvc를 빈에
등록할 수 없어서 @AutoConfigureMockMvc 추가.  
또한 각각의 테스트가 다른 테스트에 영향가지 않도록 @BeforeEach 메서드까지 추가.  
게시글 저장 시, DB에 1개의 row 데이터 저장 확인.

- 서비스 테스트 케이스

```java

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
}
```

## 게시글 조회

### 단건 조회

게시글을 저장하는 기능을 만들었으니, 게시글을 조회하는 기능도 추가해보자.  
게시글 조회는 크게 두 가지가 존재한다. 여러 개의 게시글을 한번에 조회하는 기능과 글 1개만 조회하는 기능.  
우선 특정 게시글만 가져오는 단건 조회 기능을 만들어보자.

- PostService.get(Long postId): 게시글 번호를 통한 조회 기능

```java

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    public Post get(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));
    }
}
```

`findById(postId)`로 가져온 Optional 객체에 대해 게시글이 없으면 예외 처리.

- PostController.get(Long postId)

```java

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    @GetMapping("/posts/{postId}")
    public Post get(@PathVariable Long postId) {
        return postService.get(postId);
    }
}
```

조회한 게시글을 json 타입으로 반환.

- 서비스 테스트 케이스

```java

@SpringBootTest
class PostServiceTest {

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
        Post post = postService.get(savedPost.getId());

        // then
        assertNotNull(post);
        assertEquals("글 제목", post.getTitle());
        assertEquals("글 내용...", post.getContent());
    }
}
```

- 컨트롤러 테스트 케이스

```java

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

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
}
```

### 게시글 조회 응답 클래스 분리

현재 PostController에서 게시글을 조회할 때는, PostService에서 받은 Post 엔티티를 json 형태로 리턴하고 있다.  
여기에 title 값에 글자 수 제한을 줘야하는 기능을 추가해보자.

그런데 이후에 다른 컨트롤러가 추가되고, 그 컨트롤러에서의 조회는 현재 PostController 글자 수 제한과 정책이 달라질 수 있다.  
따라서 엔티티 자체에 글자 수 제한이 들어가는 서비스 정책을 넣는 것은 굉장히 위험하다.  
따라서 서비스 정책에 맞는 응답 클래스를 따로 분리하자.

- 게시글의 title이 10글자 이하여야 하는 서비스의 응답 클래스

```java
package com.juwonjulog.api.response;

@Getter
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;

    @Builder
    public PostResponse(Long id, String title, String content) {
        this.id = id;
        this.title = title.substring(0, Math.min(title.length(), 10));
        this.content = content;
    }
}
```

컨트롤러와 서비스에서 응답값 수정. 그리고 서비스 단계에서 이전의 Post 엔티티를 PostCreate 객체로 변환.

```java

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    public PostResponse get(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();
    }
}
```

- 컨트롤러 테스트 케이스. title 10글자 제한

```java

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

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
}
```

### 모든 글 조회

모든 게시글을 한번에 조회하는 api를 만들어보자.  
단건 조회에서 Post 엔티티를 PostResponse 객체로 가져온 것과 달리, 여러개이므로 List<PostResponse> 형태로 가져와야 한다.

- PostResponse 생성자 오버로딩. 서비스에서 Post 객체를 PostResponse 객체로 변환 과정에 도움

```java

@Getter
public class PostResponse {

    public PostResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
    }
}
```

- PostService.getList(): 리스트 형태로 모든 게시글을 PostResponse 객체로 반환

```java
public class PostService {

    public List<PostResponse> getList() {
        return postRepository.findAll().stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
    }
}
```

- PostController.getList(): "/posts" Http Get Method

```java
public class PostController {

    @GetMapping("/posts")
    public List<PostResponse> getList() {
        return postService.getList();
    }
}
```

- 서비스 테스트 케이스

```java
class PostServiceTest {

    @Test
    @DisplayName("DB에 저장된 글 여러개 조회")
    void get_posts_saved_in_db() {
        // given
        postRepository.saveAll(List.of(
                Post.builder()
                        .title("title_1")
                        .content("content_1")
                        .build(),
                Post.builder()
                        .title("title_2")
                        .content("content_2")
                        .build()
        ));

        // when
        List<PostResponse> response = postService.getList();

        // then
        assertNotNull(response);
        assertEquals(2L, response.size());
    }
}
```

- 컨트롤러 테스트 케이스

```java
class PostControllerTest {

    @Test
    @DisplayName("/posts에 getList 요청 시 글 여러개 조회")
    void get_posts() throws Exception {
        // given
        Post savedPost1 = postRepository.save(Post.builder()
                .title("title_1")
                .content("content_1")
                .build());

        Post savedPost2 = postRepository.save(Post.builder()
                .title("title_2")
                .content("content_2")
                .build());

        // expected
        mockMvc.perform(get("/posts")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$[0].id").value(savedPost1.getId()))
                .andExpect(jsonPath("$[0].title").value("title_1"))
                .andExpect(jsonPath("$[0].content").value("content_1"))
                .andExpect(jsonPath("$[1].id").value(savedPost2.getId()))
                .andExpect(jsonPath("$[1].title").value("title_2"))
                .andExpect(jsonPath("$[1].content").value("content_2"))
                .andDo(print());
    }
}
```

한편 지금까지 만든 서비스는 DB가 메모리 방식이라 데이터가 남지 않았다.  
웹 브라우저에서 직접 눈으로 확인해보자.

- application.yml 설정. 데이터 확인

```
spring:
  h2:
    console:
      enabled: true
      path: /h2-console

  datasource:
    url: jdbc:h2:mem:juwonjulog
    username: sa
    password:
    driver-class-name: org.h2.Driver
```

### Pageable 페이징 처리

이전에 만든 모든 글 조회 기능은 사실 비용이 너무 많이 든다.  
한번에 너무 많은 DB를 탐색하기도 하고, 그렇게 받은 데이터를 다시 api로 전달할 때 시간과 비용 부담되므로 페이징 기능을 추가해 수정하자.

일단 간단하게 한 페이지를 조회했을 때, 최신글 순으로 5개씩 리턴하도록 구현.

- PostController

```java
import org.springframework.data.domain.Pageable;

public class PostController {

    private final PostService postService;

    @GetMapping("/posts")
    public List<PostResponse> getList(Pageable pageable) {
        return postService.getList(pageable);
    }
}
```

Pageable 인터페이스를 파라미터로 넘기면서 페이징 처리. page, sort 정보 등을 Request에 담아 서비스로 넘기기.

- PostService

```java
public class PostService {

    private final PostRepository postRepository;

    public List<PostResponse> getList(Pageable pageable) {
        return postRepository.findAll(pageable).stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
    }
}
```

- application.yml: 1페이지 요청을 0페이지 요청으로 변환 + 기본 페이징 갯수 5개 설정

```
spring:
  data:
    web:
      pageable:
        one-indexed-parameters: true
        default-page-size: 5
```

- PostController 테스트 케이스. DB에 글 30개 저장

```java
class PostControllerTest {

    @Test
    @DisplayName("/posts에 getList(1) 요청 시 1페이지 글 5개 내림차순 조회")
    void get_1_page_5_posts_desc() throws Exception {
        // given
        List<Post> requestPosts = IntStream.range(1, 31)
                .mapToObj(i -> Post.builder()
                        .title("title_" + i)
                        .content("content_" + i)
                        .build())
                .collect(Collectors.toList());
        postRepository.saveAll(requestPosts);

        // expected
        mockMvc.perform(get("/posts?page=1&sort=id,desc")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(5)))
                .andExpect(jsonPath("$[0].id").value(30))
                .andExpect(jsonPath("$[0].title").value("title_30"))
                .andExpect(jsonPath("$[0].content").value("content_30"))
                .andExpect(jsonPath("$[4].id").value(26))
                .andExpect(jsonPath("$[4].title").value("title_26"))
                .andExpect(jsonPath("$[4].content").value("content_26"))
                .andDo(print());
    }
}
```

- PostService 테스트 케이스

```java
class PostServiceTest {

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
```

### Querydsl 페이징 처리

앞서 Pageable 인터페이스를 사용해서 처리했던 페이징 기능을, Querydsl을 사용해서 보다 쉽게 개선해보자.  
또한 페이징을 할 때 페이지, 한번에 가져오는 글의 갯수 등의 조건들을 요청할 수 있는 DTO 클래스도 추가.  
우선 Querydsl은 따로 플러그인 설치가 필요하다.

- build.gradle에 dependencies 추가. 이후 Gradle 리로드

```
dependencies {

	implementation 'com.querydsl:querydsl-core'
    implementation 'com.querydsl:querydsl-jpa'
    annotationProcessor "com.querydsl:querydsl-apt:${dependencyManagement.importedProperties['querydsl.version']}:jpa"
    annotationProcessor 'jakarta.persistence:jakarta.persistence-api'
    annotationProcessor 'jakarta.annotation:jakarta.annotation-api'
}
```

이후에 Gradle Tasks/build/classes 실행을 통해 엔티티 Q클래스 생성

- build 폴더에서 QPost 생성 확인

```java
package com.juwonjulog.api.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;

import javax.annotation.processing.Generated;

import com.querydsl.core.types.Path;


/**
 * QPost is a Querydsl query type for Post
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPost extends EntityPathBase<Post> {

    private static final long serialVersionUID = 1946354179L;

    public static final QPost post = new QPost("post");

    public final StringPath content = createString("content");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath title = createString("title");

    public QPost(String variable) {
        super(Post.class, forVariable(variable));
    }

    public QPost(Path<? extends Post> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPost(PathMetadata metadata) {
        super(Post.class, metadata);
    }

}
```

- QuerydslConfig 클래스. 엔티티 매니저의 영속성 컨텍스트 등록과 JpaQueryFacotry 빈 등록 설정

```java
package com.juwonjulog.api.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Configuration
public class QuerydslConfig {

    @PersistenceContext
    public EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
```

- DTO PostSearch 클래스. 필드값이 null이면 기본값 부여

```java
package com.juwonjulog.api.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import static java.lang.Math.max;
import static java.lang.Math.min;

@Getter
@Setter
@Builder
public class PostSearch {

    private static final int MAX_SIZE = 2000;

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer size = 10;

    public long getOffset() {
        return (long) (max(page, 1) - 1) * min(size, MAX_SIZE);
    }
}

```

한편 0페이지를 요청했을 때, 기본적으로 1페이지를 리턴할 수 있도록 설정. 그리고 최대 글 가져오는 갯수도 2000개로 제한.

- PostRepositoryCustom 인터페이스

```java
package com.juwonjulog.api.repository;

import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.request.PostSearch;

import java.util.List;

public interface PostRepositoryCustom {

    List<Post> getList(PostSearch postSearch);
}
```

- PostRepositoryImpl 구현 클래스. Querydsl을 사용해서 페이징 메서드 구현

```java
package com.juwonjulog.api.repository;

import com.juwonjulog.api.domain.Post;
import com.juwonjulog.api.request.PostSearch;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.juwonjulog.api.domain.QPost.post;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Post> getList(PostSearch postSearch) {
        return jpaQueryFactory.selectFrom(post)
                .limit(postSearch.getSize())
                .offset(postSearch.getOffset())
                .orderBy(post.id.desc())
                .fetch();
    }
}
```

- PostRepository 인터페이스에 PostRepositoryCustom 확장

```java
package com.juwonjulog.api.repository;

import com.juwonjulog.api.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
}
```

- PostController 이전 페이징 기능 수정

```java
public class PostController {

    @GetMapping("/posts")
    public List<PostResponse> getList(@ModelAttribute PostSearch postSearch) {
        return postService.getList(postSearch);
    }
}
```

- PostService 이전 페이징 기능 수정

```java
public class PostService {

    public List<PostResponse> getList(PostSearch postSearch) {
        return postRepository.getList(postSearch).stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
    }
}
```

- PostController 테스트 케이스

```java
class PostControllerTest {

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
```

- PostService 테스트 케이스

```java
class PostServiceTest {

    @Test
    @DisplayName("DB에 저장된 글 1페이지 10개 조회")
    void get_1_page_posts_saved_in_db() {
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
}
```

## 게시글 수정

이번에는 이미 저장된 게시글을 불러와서, 수정하는 기능을 추가해보자.  
게시글 수정에는 1) 수정할 게시글의 식별 번호, 즉 DB 테이블의 primary id값이 필요하고, 2) 수정할 게시글의 내용이 필요하다.

- 수정할 내용을 담아 넘기는 PostEdit 요청 클래스. 이전에 글을 등록하는 PostCreate 클래스와 거의 유사함

```java
package com.juwonjulog.api.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class PostEdit {

    @NotBlank(message = "타이틀을 입력해주세요.")
    private String title;

    @NotBlank(message = "콘텐츠를 입력해주세요.")
    private String content;

    @Builder
    public PostEdit(String title, String content) {
        this.title = title;
        this.content = content;
    }
}

```

한편 넘겨받은 PostEdit 데이터를 수정할 Post 게시글에 저장해야 하는데, 이 과정에서 PostEditor라는 도메인 클래스를 사용해서 일종의 글 에디터 기능을 불러와 수정한다.  
PostEditor 클래스는 수정할 수 있는 필드들에 대해서만 정의하여, 다른 사용자가 임의로 다른 필드를 수정할 수 없게 하고, 파라미터의 순서가 바뀌어도 문제 없이 수정되도록 도와줄 수 있다.

- PostEditor 클래스

```java
package com.juwonjulog.api.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PostEditor {

    private final String title;
    private final String content;

    @Builder
    public PostEditor(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
```

- Post 클래스의 에디터 진입 메서드 toEditor() 메서드 / 수정한 PostEditor 내용을 게시글에 입력하는 edit() 메서드

```java
package com.juwonjulog.api.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    public PostEditor.PostEditorBuilder toEditor() {
        return PostEditor.builder()
                .title(this.title)
                .content(this.content);
    }

    public void edit(PostEditor postEditor) {
        this.title = postEditor.getTitle();
        this.content = postEditor.getContent();
    }
}
```

toEditor() 메서드는 Post 객체에서 title, content와 같은 내용을 빌드하지 않은 상태로 넘기는 것이 목표.  
이렇게 빌드되지 않은 PostEditor.PostEditorBuilder 객체는 서비스 단계에서 PostEdit 내용을 받아 새로운 내용으로 빌드하게 된다.

- PostService 클래스의 글 수정 edit()

```java
package com.juwonjulog.api.service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    @Transactional
    public void edit(Long postId, PostEdit postEdit) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        PostEditor.PostEditorBuilder editorBuilder = post.toEditor();

        PostEditor postEditor = editorBuilder
                .title(postEdit.getTitle())
                .content(postEdit.getContent())
                .build();

        post.edit(postEditor);
    }
}
```

- 테스트 케이스

```java

@SpringBootTest
class PostServiceTest {

    @Test
    @DisplayName("글 제목 수정")
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
                .orElseThrow(() -> new RuntimeException("글이 존재하지 않습니다. id=" + post.getId()));
        assertEquals("edited_title", editedPost.getTitle());
        assertEquals("content", editedPost.getContent());
    }

    @Test
    @DisplayName("글 내용 수정")
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
                .orElseThrow(() -> new RuntimeException("글이 존재하지 않습니다. id=" + post.getId()));
        assertEquals("title", editedPost.getTitle());
        assertEquals("edited_content", editedPost.getContent());
    }
}
```

- PostController 클래스의 글 수정 라우팅

```java

@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController {

    @PatchMapping("/posts/{postId}")
    public void edit(@PathVariable Long postId, @RequestBody @Valid PostEdit postEdit) {
        postService.edit(postId, postEdit);
    }
}
```

- 테스트 케이스

```java

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

    @Test
    @DisplayName("글 제목 수정 요청")
    void edit_post_title_request() throws Exception {
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
    @DisplayName("글 내용 수정 요청")
    void edit_post_content_request() throws Exception {
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
}
```

## 게시글 삭제

CRUD의 마지막, 삭제 기능을 추가해보자.  
게시글 삭제 기능은 Post의 id를 넘기는 방식으로 특정 게시글을 불러오고, 따로 바디에 어떤 내용을 담지는 않겠다.

- PostService 클래스의 글 삭제 메서드 delete()

```java
public class PostService {

    public void delete(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 글입니다."));

        postRepository.delete(post);
    }
}
```

- 서비스 테스트 케이스

```java

@SpringBootTest
class PostServiceTest {

    @Test
    @DisplayName("글 삭제")
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
}
```

- PostController 클래스의 글 삭제 라우팅

```java
public class PostController {

    @DeleteMapping("/posts/{postId}")
    public void delete(@PathVariable Long postId) {
        postService.delete(postId);
    }
}
```

- 컨트롤러 테스트 케이스

```java

@SpringBootTest
class PostControllerTest {

    @Test
    @DisplayName("글 삭제 요청")
    void delete_post_request() throws Exception {
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
}
```

## 예외 처리

지금까지 만든 게시글 조회, 수정, 삭제 기능은 존재하지 않는 게시글을 불러오면 IllegalArgumentException을 던졌다.  
문법적으로 틀린 것은 아니지만, 클라이언트가 예외에 당면했을 때 직관적이지 못하다.  
따라서 블로그 서비스 용도로 만든 예외를 생성해서 던져주도록 하자.

### 존재하지 않는 게시글 예외 처리

- PostNotFound 클래스

```java
package com.juwonjulog.api.exception;

public class PostNotFound extends RuntimeException {

    private static final String MESSAGE = "존재하지 않는 글입니다.";

    public PostNotFound() {
        super(MESSAGE);
    }
}
```

우선은 MESSAGE만 생성됨과 동시에 담기도록 생성자 처리. 이후에 필요하다면, Throwable cause까지 담을 수 있도록 코드를 추가하자.

- PostService의 각 기능들 예외 처리 수정

```java
public class PostService {

    public PostResponse get(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFound::new);

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();
    }

    public void edit(Long postId, PostEdit postEdit) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFound::new);

        PostEditor.PostEditorBuilder editorBuilder = post.toEditor();

        PostEditor postEditor = editorBuilder
                .title(postEdit.getTitle())
                .content(postEdit.getContent())
                .build();

        post.edit(postEditor);
    }

    public void delete(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFound::new);

        postRepository.delete(post);
    }
}
```

- 테스트 케이스

```java

@SpringBootTest
class PostServiceTest {

    @Test
    @DisplayName("DB에 존재하지 않는 게시글 단건 조회 시 예외 출력")
    void get_not_exists_post() {
        // expected
        Throwable exception = assertThrows(PostNotFound.class, () -> postService.get(1L));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
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
    @DisplayName("DB에 존재하지 않는 게시글 삭제 시 예외 출력")
    void delete_not_exists_post() {
        // expected
        Throwable exception = assertThrows(PostNotFound.class, () -> postService.delete(1L));
        assertEquals("존재하지 않는 글입니다.", exception.getMessage());
    }
}
```

### 비즈니스 최상위 예외 클래스

지난번에 만든 게시글 예외 처리는 게시글을 조회, 수정, 삭제할 때 존재하지 않는 게시글을 불러올 때만 발생시키는 예외 처리였다.  
그리고 서비스 단계에서만 예외 테스트 코드를 짰었는데, 컨트롤러 단계에서도 이 예외 처리를 적용하려면 이전에 만든 ExceptionController 클래스에 ExceptionHandler를 또 추가해줘야 한다.

한편 과연 앞으로도 블로그 기능을 추가하다보면 이 존재하지 않는 예외 처리만 존재할까? 계속해서 다양한 방식의 예외 처리가 필요할 것이다.  
그러므로 그때마다 ExceptionController에 메서드 별로 하나하나 추가해줘야 하는 번거로움이 생긴다.  
같은 비즈니스 클래스에 해당한다면, 최상위 예외 클래스로 추상 클래스를 만든 뒤 이를 상속하는 방식으로 예외 클래스들을 만들어보자.

- JuwonjulogException 클래스: 최상위 블로그 비즈니스 예외 클래스

```java
package com.juwonjulog.api.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class JuwonjulogException extends RuntimeException {

    public final Map<String, String> validation = new HashMap<>();

    public JuwonjulogException(String message) {
        super(message);
    }

    public JuwonjulogException(String message, Throwable cause) {
        super(message, cause);
    }

    public abstract int getStatusCode();

    public void addValidation(String fieldName, String message) {
        validation.put(fieldName, message);
    }
}
```

Exception의 메시지를 기본 생성자로 받으며, 예외 클래스 별로 상태 코드와 validation 값을 넣을 수 있는 메서드를 추가.

- PostNotFound 클래스: 존재하지 않는 게시글 예외 처리

```java
package com.juwonjulog.api.exception;

public class PostNotFound extends JuwonjulogException {

    private static final String MESSAGE = "존재하지 않는 글입니다.";

    public PostNotFound() {
        super(MESSAGE);
    }

    @Override
    public int getStatusCode() {
        return 404;
    }
}
```

- InvalidRequest 클래스: 클라이언트의 잘못된 요청 예외 처리

```java
package com.juwonjulog.api.exception;

public class InvalidRequest extends JuwonjulogException {

    private static final String MESSAGE = "잘못된 요청입니다.";

    public InvalidRequest() {
        super(MESSAGE);
    }

    public InvalidRequest(String fieldName, String message) {
        super(MESSAGE);
        addValidation(fieldName, message);
    }

    @Override
    public int getStatusCode() {
        return 400;
    }
}
```

클라이언트에 예외가 터졌을 때, 구체적 이유를 알 수 있는 validation 값을 넣어준다.

- PostCreate 클래스: 게시글 작성 시 제목에 욕을 넣을 수 없도록 제한

```java
package com.juwonjulog.api.request;

public class PostCreate {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @Builder
    public PostCreate(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void validate() {
        if (title.contains("욕")) {
            throw new InvalidRequest("title", "제목에 욕을 포함할 수 없습니다.");
        }
    }
}
```

- PostController 클래스: 게시글 작성 시 제목에 욕이 들어갔는지 검증

```java
public class PostController {

    @PostMapping("/posts")
    public void post(@RequestBody @Valid PostCreate request) {
        request.validate();
        postService.write(request);
    }
}
```

- ExceptionController 클래스: 최상위 비즈니스 예외 클래스 잡아주기

```java

@ControllerAdvice
public class ExceptionController {

    @ResponseBody
    @ExceptionHandler(JuwonjulogException.class)
    public ResponseEntity<ErrorResponse> juwonjulogExceptionHandler(JuwonjulogException e) {
        int statusCode = e.getStatusCode();

        ErrorResponse body = ErrorResponse.builder()
                .code(String.valueOf(statusCode))
                .message(e.getMessage())
                .validation(e.getValidation())
                .build();

        return ResponseEntity.status(statusCode).body(body);
    }
}
```

비즈니스 예외가 터졌을 때, 각 비즈니스 예외의 statusCode와 message를 직접 가져와 body에 삽입.  
그리고 validation 필드와 값이 있다면, validation까지 넣어준다.

- ErrorResponse 클래스: 생성자에 validation 값을 넣어주도록 코드 수정. validation이 없으면 생성해서 주입

```java
public class ErrorResponse {

    private final String code;
    private final String message;
    private final Map<String, String> validation;

    @Builder
    public ErrorResponse(String code, String message, Map<String, String> validation) {
        this.code = code;
        this.message = message;
        this.validation = validation != null ? validation : new HashMap<>();
    }
}
```

- PostController 테스트 케이스

```java

@AutoConfigureMockMvc
@SpringBootTest
class PostControllerTest {

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
    @DisplayName("존재하지 않는 게시글 단건 조회")
    void get_nonexistent_post() throws Exception {
        // expected
        mockMvc.perform(get("/posts/{postId}", 1L)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
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
    @DisplayName("존재하지 않는 게시글 삭제")
    void delete_nonexistent_post() throws Exception {
        // expected
        mockMvc.perform(delete("/posts/{postId}", 1L)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andDo(print());
    }
}
```


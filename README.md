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

### 여러개 조회

게시글을 한번에 여러개 조회하는 api를 만들어보자.  
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

```java
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


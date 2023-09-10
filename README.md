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

- 테스트 케이스

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

컨트롤러 - 서비스 - 레포지토리에 이르는 api 전반적 테스트를 하기 위해, @WebMvcTest에서 @SpringBootTest 수정. 그런데 @SpringBootTest 애노테이션만으로는 MockMvc를 빈에 등록할 수 없어서 @AutoConfigureMockMvc 추가.  
또한 각각의 테스트가 다른 테스트에 영향가지 않도록 @BeforeEach 메서드까지 추가.  
게시글 저장 시, DB에 1개의 row 데이터 저장 확인.


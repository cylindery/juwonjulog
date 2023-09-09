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


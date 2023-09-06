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


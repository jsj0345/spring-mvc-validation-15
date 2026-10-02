# Spring MVC Validation 15

Spring MVC에서 입력값 검증과 오류 처리 방법을 학습하고 예제 코드와 문서로 정리한 저장소입니다.

`BindingResult`, Spring Validator, Bean Validation을 중심으로 검증 오류를 처리하고 화면과 HTTP API에 적용하는 흐름을 학습했습니다.

## 학습 목적

Spring MVC에서 사용자 입력값을 검증하고, 오류가 발생했을 때 입력값과 오류 메시지를 함께 처리하는 흐름을 이해하기 위해 정리했습니다.

직접 검증하는 방식부터 Spring이 제공하는 검증 기능으로 확장되는 과정을 예제 코드로 확인했습니다.

## 학습 내용

- `BindingResult`를 활용한 검증 오류 처리
- `FieldError`, `ObjectError`와 오류 메시지 관리
- Spring `Validator`와 `@Validated`
- Bean Validation과 검증 애노테이션
- 등록/수정 Form 객체 분리
- `@RequestBody`를 활용한 API 요청 검증
- [개념 정리 파일 보기](./src/main/docs)

## 디렉터리 구조

```text
spring-mvc-validation-15
├── src
│   ├── main
│   │   ├── docs
│   │   │   ├── 01-validation.md
│   │   │   └── 02-bean-validation.md
│   │   ├── java
│   │   │   └── hello
│   │   │       └── itemservice
│   │   │           ├── domain
│   │   │           └── web
│   │   │               └── validation
│   │   │                   ├── form
│   │   │                   └── Controller
│   │   └── resources
│   │       ├── templates
│   │       ├── errors.properties
│   │       └── messages.properties
│   └── test
├── build.gradle
├── gradlew
├── gradlew.bat
└── settings.gradle
```

## 학습 포인트

- `BindingResult`를 통해 바인딩 오류와 검증 오류를 처리하는 흐름을 학습했습니다.
- 오류 코드와 메시지를 분리하고 `Validator`, Bean Validation을 활용해 검증 로직을 단순화했습니다.
- 등록과 수정의 요구사항에 따라 Form 객체를 분리하고 각각 검증하는 방식을 익혔습니다.
- 폼 요청과 JSON API 요청에서 검증 및 타입 변환 오류가 처리되는 차이를 확인했습니다.

## 실행 환경

- Java 11
- Spring Boot 2.4.4
- Spring MVC
- Thymeleaf
- Bean Validation
- Gradle
- Lombok
- JUnit 5
- IntelliJ IDEA

## 참고

- 코드 출처 : 스프링 MVC 2편 - 백엔드 웹 개발 활용 기술
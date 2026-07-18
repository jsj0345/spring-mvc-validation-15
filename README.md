# Spring MVC Validation 15

Spring MVC에서 입력값 검증과 오류 처리 과정을 학습하고 예제 코드로 정리한 저장소입니다.

직접 검증 로직을 작성하는 방식부터 `BindingResult`, `FieldError`, `ObjectError`, 오류 코드와 메시지 분리, `Validator`, Bean Validation, 검증 그룹, Form 전송 객체 분리, HTTP API 검증까지 단계적으로 학습했습니다.

## 학습 목적

사용자가 입력한 값에 오류가 있을 때 Controller에서 검증 결과를 처리하고, 입력값을 유지한 상태로 오류 메시지를 화면에 다시 표시하는 Spring MVC의 검증 흐름을 이해하기 위해 정리했습니다.

검증 로직을 직접 구현하는 방식에서 시작해 Spring이 제공하는 `BindingResult`와 `Validator`, Bean Validation으로 확장하고, 등록과 수정의 요구사항이 달라지는 경우 별도의 Form 객체를 사용하는 방식까지 비교하며 검증 구조를 이해하는 데 중점을 두었습니다.

## 학습 내용

* Map을 활용한 직접 검증과 오류 정보 관리
* 필드 오류와 글로벌 오류 처리
* Thymeleaf의 Safe Navigation Operator를 활용한 null 안전 처리
* `BindingResult`를 활용한 검증 오류 저장과 조회
* `FieldError`, `ObjectError`를 활용한 오류 등록
* 오류 발생 시 사용자가 입력한 값 유지
* `th:errors`, `th:errorclass`, `#fields`를 활용한 검증 오류 출력
* 오류 코드와 오류 메시지 분리
* `errors.properties`를 활용한 검증 메시지 관리
* `MessageCodesResolver`의 오류 코드 생성 규칙
* `rejectValue()`, `reject()`를 활용한 오류 코드 등록
* `typeMismatch` 바인딩 오류와 사용자 입력값 보존
* Spring `Validator` 인터페이스를 활용한 검증 로직 분리
* `@InitBinder`와 `WebDataBinder`를 활용한 Validator 등록
* `@Validated`를 활용한 Validator 실행
* Bean Validation과 `spring-boot-starter-validation`
* `@NotBlank`, `@NotNull`, `@Range`, `@Max` 검증 애노테이션
* Spring MVC와 Bean Validation 통합
* Bean Validation 오류 코드와 메시지 처리
* 복합 조건 검증을 위한 글로벌 오류 직접 처리
* 등록과 수정의 서로 다른 검증 요구사항 처리
* `SaveCheck`, `UpdateCheck`를 활용한 검증 그룹 방식 학습
* `ItemSaveForm`, `ItemUpdateForm`을 활용한 Form 전송 객체 분리
* Form 객체 검증 후 도메인 객체 `Item`으로 변환하는 흐름
* `@RequestBody`와 `@Validated`를 활용한 HTTP API 요청 검증
* HTTP 메시지 컨버터 변환 실패와 Bean Validation 오류의 차이

## 디렉터리 구조

    spring-mvc-validation-15
    ├── gradle
    │   └── wrapper
    ├── src
    │   ├── main
    │   │   ├── docs
    │   │   │   ├── 01-validation.md
    │   │   │   └── 02-bean-validation.md
    │   │   ├── java
    │   │   │   └── hello
    │   │   │       └── itemservice
    │   │   │           ├── domain
    │   │   │           │   └── item
    │   │   │           │       ├── Item.java
    │   │   │           │       ├── ItemRepository.java
    │   │   │           │       ├── SaveCheck.java
    │   │   │           │       └── UpdateCheck.java
    │   │   │           ├── web
    │   │   │           │   └── validation
    │   │   │           │       ├── form
    │   │   │           │       │   ├── ItemSaveForm.java
    │   │   │           │       │   └── ItemUpdateForm.java
    │   │   │           │       ├── ItemValidator.java
    │   │   │           │       ├── ValidationItemApiController.java
    │   │   │           │       ├── ValidationItemControllerV1.java
    │   │   │           │       ├── ValidationItemControllerV2.java
    │   │   │           │       ├── ValidationItemControllerV3.java
    │   │   │           │       └── ValidationItemControllerV4.java
    │   │   │           ├── ItemServiceApplication.java
    │   │   │           └── TestDataInit.java
    │   │   └── resources
    │   │       ├── static
    │   │       │   ├── css
    │   │       │   │   └── bootstrap.min.css
    │   │       │   └── index.html
    │   │       ├── templates
    │   │       │   └── validation
    │   │       │       ├── v1
    │   │       │       ├── v2
    │   │       │       ├── v3
    │   │       │       └── v4
    │   │       ├── application.properties
    │   │       ├── errors.properties
    │   │       ├── messages.properties
    │   │       └── messages_en.properties
    │   └── test
    │       └── java
    │           └── hello
    │               └── itemservice
    │                   ├── domain
    │                   │   └── item
    │                   │       └── ItemRepositoryTest.java
    │                   ├── message
    │                   │   └── MessageSourceTest.java
    │                   ├── validation
    │                   │   ├── BeanValidationTest.java
    │                   │   └── MessageCodesResolverTest.java
    │                   └── ItemServiceApplicationTests.java
    ├── build.gradle
    ├── gradlew
    ├── gradlew.bat
    └── settings.gradle

## 학습 포인트

* `ValidationItemControllerV1`에서는 검증 오류를 `Map`에 직접 저장하고 Model에 전달해 필드 오류와 글로벌 오류를 화면에 출력하는 기본 검증 흐름을 확인했습니다.
* `errors?.containsKey(...)` 형태의 Safe Navigation Operator를 사용해 최초 등록 화면처럼 오류 객체가 없는 경우에도 NullPointerException 없이 화면을 처리하는 방식을 학습했습니다.
* `ValidationItemControllerV2`에서는 `BindingResult`에 `FieldError`와 `ObjectError`를 추가하고, Thymeleaf의 `th:errors`, `th:errorclass`, `#fields.hasGlobalErrors()`를 통해 오류를 렌더링하는 방식으로 확장했습니다.
* `FieldError`의 rejected value를 통해 검증에 실패해도 사용자가 입력한 값이 화면에 유지되는 흐름과, 타입 변환에 실패한 경우 Spring이 생성하는 `typeMismatch` 오류를 확인했습니다.
* `errors.properties`와 `MessageCodesResolver`를 사용해 객체명, 필드명, 타입에 따라 구체적인 메시지부터 범용 메시지까지 우선순위로 조회하는 오류 메시지 처리 방식을 학습했습니다.
* `BindingResult.rejectValue()`와 `reject()`를 사용해 오류 코드를 간결하게 등록하고, Spring이 생성한 메시지 코드 목록을 기반으로 메시지를 찾는 과정을 확인했습니다.
* `ItemValidator`에 검증 로직을 분리하고 `supports()`와 `validate()`를 구현했으며, `@InitBinder`를 통해 `WebDataBinder`에 Validator를 등록하고 `@Validated`로 실행하는 흐름을 학습했습니다.
* `spring-boot-starter-validation`을 추가해 Bean Validation을 사용하고, 필드 검증에 `@NotBlank`, `@NotNull`, `@Range`, `@Max`를 적용하는 방법을 학습했습니다.
* Bean Validation이 적용되기 전에 `@ModelAttribute` 바인딩과 타입 변환이 먼저 수행되며, 바인딩에 성공한 필드를 대상으로 검증이 진행되는 순서를 확인했습니다.
* 가격과 수량의 곱처럼 여러 필드가 함께 필요한 복합 조건은 `bindingResult.reject()`를 사용해 글로벌 오류로 직접 처리했습니다.
* 등록과 수정의 검증 조건이 다른 문제를 해결하기 위해 `SaveCheck`, `UpdateCheck`를 활용한 검증 그룹 방식을 학습한 뒤, 별도의 `ItemSaveForm`, `ItemUpdateForm` 객체로 요청 데이터를 분리하는 방식으로 확장했습니다.
* `ValidationItemControllerV4`에서는 등록 요청을 `ItemSaveForm`, 수정 요청을 `ItemUpdateForm`으로 각각 검증하고, 검증이 성공한 뒤 Form 객체의 값을 사용해 `Item` 도메인 객체를 생성하거나 수정하는 흐름을 적용했습니다.
* `ValidationItemApiController`에서는 `@RequestBody @Validated ItemSaveForm`으로 JSON 요청을 검증하고, JSON 자체의 타입 변환에 실패하면 Controller와 Validator가 실행되기 전에 `HttpMessageNotReadableException`이 발생하는 흐름을 학습했습니다.
* HTTP 메시지 변환에는 성공했지만 Bean Validation에 실패한 경우 `BindingResult`에 `FieldError` 또는 `ObjectError`가 담기는 차이를 확인했습니다.

## 실행 환경

* Java 11
* Spring Boot 2.4.4
* Spring MVC
* Thymeleaf
* Bean Validation
* Gradle
* Lombok
* JUnit 5
* AssertJ
* IntelliJ IDEA
* HTML
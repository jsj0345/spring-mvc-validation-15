# Bean Validation 복습

> 반복되는 필드 검증을 애노테이션으로 표현하고, Spring MVC의 바인딩·오류 처리와 연결하는 과정을 정리했다.

## 1. Bean Validation이 해결하는 문제

필수값, 숫자 범위, 최대 길이처럼 반복되는 규칙을 모든 컨트롤러에서 조건문으로 작성하면 코드가 중복된다.

Bean Validation은 일반적인 검증 규칙을 애노테이션으로 선언할 수 있도록 표준화한 방식이다.

```java
public class Item {

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1000, max = 1000000)
    private Integer price;

    @NotNull
    @Max(9999)
    private Integer quantity;
}
```

### 사용한 제약 조건

| 애노테이션 | 의미 |
|---|---|
| `@NotBlank` | `null`, 빈 문자열, 공백 문자열을 허용하지 않음 |
| `@NotNull` | `null`을 허용하지 않음 |
| `@Range` | 지정한 최솟값과 최댓값 사이만 허용 |
| `@Max` | 지정한 값 이하만 허용 |

Bean Validation은 검증 규칙을 모델 가까이에 모을 수 있지만 모든 업무 규칙을 애노테이션으로 해결한다는 뜻은 아니다.

---

## 2. 의존성과 순수 검증 실행

Spring Boot 프로젝트에서는 검증 스타터를 추가한다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-validation'
```

Spring과 연결하지 않고 검증기 자체를 사용할 수도 있다.

```java
ValidatorFactory factory =
        Validation.buildDefaultValidatorFactory();

Validator validator = factory.getValidator();

Set<ConstraintViolation<Item>> violations =
        validator.validate(item);
```

`violations`가 비어 있으면 제약 조건을 모두 통과한 것이다. 오류가 있으면 어떤 프로퍼티에서 어떤 규칙을 위반했는지 확인할 수 있다.

이 방식은 Bean Validation 자체의 동작을 확인하는 테스트에는 유용하지만, Spring MVC에서는 검증기를 직접 생성하지 않는다.

---

## 3. Spring MVC와 통합하기

Spring Boot는 검증 라이브러리를 발견하면 Bean Validator를 Spring 검증 체계에 연결한다.

컨트롤러에서는 `@Valid` 또는 `@Validated`를 붙인다.

```java
@PostMapping("/add")
public String add(
        @Validated @ModelAttribute Item item,
        BindingResult bindingResult
) {
    if (bindingResult.hasErrors()) {
        return "validation/addForm";
    }

    itemRepository.save(item);
    return "redirect:/items";
}
```

```text
@Valid
→ Bean Validation 표준 애노테이션

@Validated
→ Spring이 제공하는 검증 애노테이션
```

검증 오류는 `FieldError` 또는 `ObjectError` 형태로 `BindingResult`에 저장된다.

---

## 4. 바인딩과 검증의 실행 순서

요청값이 객체에 들어가기 전에 먼저 타입 변환이 필요하다.

```text
요청 파라미터
→ Item 프로퍼티 타입으로 변환
→ 변환 성공한 필드에 Bean Validation 적용
```

가격 필드가 `Integer`인데 `"abc"`가 들어오면 타입 변환부터 실패한다.

```text
타입 변환 실패
→ typeMismatch FieldError 생성
→ 해당 필드에는 Bean Validation을 추가로 수행하지 않음
```

객체에 정상적인 값이 들어가야 범위나 최댓값을 검사할 수 있기 때문이다.

---

## 5. Bean Validation 오류 메시지

제약 조건 오류도 Spring의 메시지 코드 규칙을 따른다.

`@NotBlank`가 `item.itemName`에서 실패하면 다음과 같은 후보가 만들어질 수 있다.

```text
NotBlank.item.itemName
NotBlank.itemName
NotBlank.java.lang.String
NotBlank
```

`@Range`도 같은 구조로 코드가 생성된다.

```text
Range.item.price
Range.price
Range.java.lang.Integer
Range
```

`errors.properties`에 메시지를 등록할 수 있다.

```properties
NotBlank=필수 입력값입니다.
Range={0}은 {2}부터 {1} 사이여야 합니다.
Max={0}은 최대 {1}까지 입력할 수 있습니다.
```

메시지를 찾는 흐름은 다음과 같다.

```text
1. Spring이 만든 메시지 코드 조회
2. 애노테이션의 message 속성 확인
3. 검증 구현체의 기본 문구 사용
```

특정 필드에만 다른 문구가 필요하면 더 구체적인 코드를 추가하면 된다.

---

## 6. 여러 필드를 함께 검사하는 규칙

가격과 수량을 곱한 총액처럼 여러 필드가 함께 관련된 조건은 단순 필드 애노테이션으로 표현하기 어렵다.

객체 단위 검증 애노테이션을 사용할 수 있지만 복잡한 표현식은 읽기와 유지보수가 어려울 수 있다.

이 예제에서는 필드별 규칙은 Bean Validation으로 처리하고, 복합 규칙은 Java 코드로 등록한다.

```java
if (item.getPrice() != null
        && item.getQuantity() != null) {
    int total =
            item.getPrice() * item.getQuantity();

    if (total < 10_000) {
        bindingResult.reject(
            "totalPriceMin",
            new Object[]{10000, total},
            null
        );
    }
}
```

```text
일반적인 단일 필드 규칙
→ Bean Validation

여러 값이 연결된 업무 규칙
→ 필요한 위치에서 직접 검증
```

둘 다 최종 결과는 `BindingResult`에 모을 수 있다.

---

## 7. 등록과 수정에 같은 규칙을 적용할 때 생기는 충돌

등록과 수정은 비슷해 보여도 필요한 데이터가 다를 수 있다.

예시:

```text
등록
→ id가 아직 없어도 됨
→ 수량은 9,999 이하

수정
→ 기존 id가 필요함
→ 수량 제한은 등록과 다를 수 있음
```

하나의 `Item` 클래스에 모든 애노테이션을 붙이면 한쪽 요구사항을 맞추는 순간 다른 쪽 검증이 깨질 수 있다.

```java
@NotNull
private Long id;
```

이 규칙은 수정에는 필요하지만 등록 시점에는 아직 ID가 없기 때문에 등록을 막을 수 있다.

반대로 등록용 수량 제한을 제거하면 수정은 통과하지만 등록 규칙을 지키지 못한다.

---

## 8. 검증 그룹

하나의 클래스에서 상황별 제약 조건을 나누기 위해 그룹을 사용할 수 있다.

```java
public interface SaveCheck {
}

public interface UpdateCheck {
}
```

```java
public class Item {

    @NotNull(groups = UpdateCheck.class)
    private Long id;

    @NotBlank(
        groups = {
            SaveCheck.class,
            UpdateCheck.class
        }
    )
    private String itemName;

    @Max(
        value = 9999,
        groups = SaveCheck.class
    )
    private Integer quantity;
}
```

컨트롤러에서 실행할 그룹을 지정한다.

```java
@Validated(SaveCheck.class)
@ModelAttribute Item item
```

```java
@Validated(UpdateCheck.class)
@ModelAttribute Item item
```

그룹을 사용하면 같은 모델에서 검증 규칙을 구분할 수 있다. 다만 모델 애노테이션과 컨트롤러 설정이 복잡해지고, 등록과 수정의 입력 구조가 크게 다르면 그룹만으로 구분하는 방식이 읽기 어려워질 수 있다.

---

## 9. 폼 전송 객체를 분리하는 이유

웹 폼에서 전달되는 데이터가 도메인 객체와 항상 같지는 않다.

등록과 수정용 입력 객체를 따로 만들면 각 요청에 필요한 필드와 검증 규칙을 독립적으로 표현할 수 있다.

```java
public class ItemSaveForm {

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1_000, max = 1_000_000)
    private Integer price;

    @NotNull
    @Max(9_999)
    private Integer quantity;
}
```

```java
public class ItemUpdateForm {

    @NotNull
    private Long id;

    @NotBlank
    private String itemName;

    @NotNull
    @Range(min = 1_000, max = 1_000_000)
    private Integer price;

    private Integer quantity;
}
```

처리 흐름:

```text
등록 폼
→ ItemSaveForm
→ 검증
→ Item 생성
→ Repository 저장

수정 폼
→ ItemUpdateForm
→ 검증
→ Item 수정 값으로 변환
→ Repository 갱신
```

### 장점

- 등록과 수정 규칙이 서로 섞이지 않는다.
- 화면에 필요한 필드만 노출할 수 있다.
- 도메인 객체가 웹 요청 형식에 직접 묶이지 않는다.

### 비용

- 폼 객체에서 도메인 객체로 변환하는 코드가 필요하다.
- 클래스 수가 늘어난다.

입력 구조가 단순할 때는 도메인 객체를 바로 사용할 수 있지만, 요구사항이 갈라지기 시작하면 폼 객체 분리가 더 명확하다.

---

## 10. `@ModelAttribute` 이름 주의

폼 객체를 사용하면 기본 모델 이름도 클래스명을 기준으로 바뀐다.

```java
@ModelAttribute ItemSaveForm form
```

기본 이름은 `itemSaveForm`이 될 수 있다.

기존 뷰가 다음 이름을 기대한다면:

```html
<form th:object="${item}">
```

컨트롤러에서 모델 이름을 명시한다.

```java
@Validated
@ModelAttribute("item")
ItemSaveForm form
```

폼 객체 타입을 바꿔도 템플릿의 `th:object` 이름은 유지할 수 있다.

---

## 11. 수정 요청에도 검증 적용하기

수정 처리에서도 검증 대상 바로 뒤에 `BindingResult`를 둔다.

```java
@PostMapping("/{itemId}/edit")
public String edit(
        @PathVariable Long itemId,
        @Validated
        @ModelAttribute("item")
        ItemUpdateForm form,
        BindingResult bindingResult
) {
    if (bindingResult.hasErrors()) {
        return "validation/editForm";
    }

    Item updateParam = new Item();
    updateParam.setItemName(form.getItemName());
    updateParam.setPrice(form.getPrice());
    updateParam.setQuantity(form.getQuantity());

    itemRepository.update(itemId, updateParam);

    return "redirect:/items/{itemId}";
}
```

검증 실패 시 수정 폼으로 돌아가고, 성공한 경우에만 저장소를 갱신한다.

---

## 12. JSON 요청과 Bean Validation

Bean Validation은 `@RequestBody`로 받는 JSON 객체에도 적용할 수 있다.

```java
@PostMapping("/api/items")
public Object addApi(
        @RequestBody
        @Validated
        ItemSaveForm form,
        BindingResult bindingResult
) {
    if (bindingResult.hasErrors()) {
        return bindingResult.getAllErrors();
    }

    return form;
}
```

하지만 폼 바인딩과 JSON 변환은 실패 시점이 다르다.

### JSON을 객체로 만들 수 없는 경우

```json
{
  "itemName": "sample",
  "price": "not-number",
  "quantity": 10
}
```

`price`를 `Integer`로 역직렬화하지 못하면 `HttpMessageConverter` 단계에서 객체 생성이 실패한다.

```text
JSON 파싱 실패
→ 컨트롤러 호출 전 예외
→ Bean Validation 실행되지 않음
```

### 객체 생성은 됐지만 제약 조건을 위반한 경우

```json
{
  "itemName": "sample",
  "price": 1000,
  "quantity": 10000
}
```

객체 변환이 성공한 뒤 `@Max` 검증이 실행되고 오류가 `BindingResult`에 들어간다.

---

## 13. API 오류 객체를 그대로 반환하는 한계

학습 단계에서는 다음처럼 오류 목록을 바로 반환할 수 있다.

```java
return bindingResult.getAllErrors();
```

그러면 `codes`, `arguments`, `rejectedValue`, `defaultMessage` 등 Spring 내부 구조가 JSON에 포함된다.

이 결과는 검증 과정을 확인하기에는 좋지만 API 응답 형식으로는 지나치게 내부 구현에 의존한다.

문서의 학습 범위에서 확인한 핵심은 다음과 같다.

```text
Thymeleaf
→ 오류 코드로 최종 메시지를 선택해 화면 출력

API에서 getAllErrors() 직접 반환
→ FieldError와 ObjectError 전체 구조가 JSON으로 노출
```

따라서 실제 API에서는 필요한 오류 코드, 필드명, 사용자 메시지만 골라 별도 응답 객체로 변환하는 편이 명확하다.

---

## 14. 내가 구분해서 기억할 기준

| 상황 | 선택 |
|---|---|
| 필수값·범위·최댓값 같은 공통 규칙 | Bean Validation 애노테이션 |
| Spring MVC 폼에서 검증 실행 | `@Valid` 또는 `@Validated` |
| 오류 결과 확인 | `BindingResult` |
| 여러 필드가 결합된 규칙 | 직접 검증 후 `reject()` |
| 등록·수정 규칙이 조금 다름 | 검증 그룹 검토 |
| 등록·수정 입력 구조가 크게 다름 | 전용 Form 객체 분리 |
| Form 파라미터 검증 | `@ModelAttribute` |
| JSON 본문 검증 | `@RequestBody` |
| JSON 타입 변환 실패 | 메시지 컨버터 단계 예외 |

## 핵심 정리

- Bean Validation은 반복되는 검증 규칙을 애노테이션으로 선언한다.
- Spring Boot는 Bean Validator를 Spring MVC 검증 흐름에 자동으로 연결한다.
- 바인딩에 성공한 필드에만 Bean Validation이 적용된다.
- 제약 조건 이름을 기반으로 단계적인 오류 코드가 생성된다.
- 필드 검증은 애노테이션에 맡기고 복합 업무 규칙은 직접 검사할 수 있다.
- 등록과 수정의 규칙이 다르면 그룹을 사용할 수 있지만 복잡도가 증가한다.
- 입력 구조가 달라지면 저장용·수정용 Form 객체를 분리하는 방식이 더 분명하다.
- `@RequestBody`에서는 JSON 객체 생성 실패와 Bean Validation 실패를 구분해야 한다.
- API에서 Spring 오류 객체를 그대로 반환하면 내부 구조가 노출되므로 별도 오류 응답 형태가 필요하다.

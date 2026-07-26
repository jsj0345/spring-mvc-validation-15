# Spring MVC 검증 흐름 복습

> 사용자가 보낸 값을 저장하기 전에 오류를 발견하고, 입력값과 오류 메시지를 다시 화면에 보여 주는 과정을 정리했다.

## 1. 검증을 직접 구현할 때

가장 단순한 방법은 컨트롤러에서 조건을 검사한 뒤 오류를 별도 자료구조에 모으는 것이다.

```java
@PostMapping("/add")
public String add(
        @ModelAttribute Item item,
        Model model
) {
    Map<String, String> errors = new HashMap<>();

    if (!StringUtils.hasText(item.getItemName())) {
        errors.put("itemName", "상품명을 입력해야 합니다.");
    }

    if (item.getPrice() == null
            || item.getPrice() < 1000
            || item.getPrice() > 1000000) {
        errors.put("price", "가격 범위를 확인해 주세요.");
    }

    if (!errors.isEmpty()) {
        model.addAttribute("errors", errors);
        return "validation/addForm";
    }

    itemRepository.save(item);
    return "redirect:/items";
}
```

필드별 오류는 필드명을 키로 사용할 수 있다. 가격과 수량을 함께 계산하는 규칙처럼 한 필드에만 연결하기 어려운 오류는 별도 키로 분리한다.

```java
int total = item.getPrice() * item.getQuantity();

if (total < 10000) {
    errors.put(
        "globalError",
        "가격과 수량을 곱한 금액이 너무 작습니다."
    );
}
```

### 직접 처리 방식의 장점

- 오류 저장 구조가 단순하다.
- 검증 과정이 코드에 그대로 보여 학습하기 쉽다.
- 작은 화면에서는 빠르게 구현할 수 있다.

### 한계

- 필드명과 오류 키를 직접 맞춰야 한다.
- 화면마다 오류 출력 조건이 반복된다.
- 타입 변환 실패를 자연스럽게 다루기 어렵다.
- 사용자가 잘못 입력한 원래 값을 보존하는 기능을 직접 만들어야 한다.
- 검증 규칙이 늘면 컨트롤러가 지나치게 커진다.

---

## 2. `BindingResult`가 맡는 역할

Spring MVC는 바인딩과 검증 오류를 담는 객체로 `BindingResult`를 제공한다.

```java
@PostMapping("/add")
public String add(
        @ModelAttribute Item item,
        BindingResult bindingResult
) {
    // 검증
    return "validation/addForm";
}
```

`BindingResult`는 검증 대상 바로 다음에 위치해야 한다.

```text
@ModelAttribute Item item
→ BindingResult bindingResult
```

숫자 필드에 문자를 입력하는 것처럼 객체 변환이 실패해도 `BindingResult`가 있으면 Spring이 오류 정보를 넣은 뒤 컨트롤러를 호출할 수 있다.

```text
BindingResult 없음
→ 바인딩 실패 시 요청 처리가 중단될 수 있음

BindingResult 있음
→ FieldError를 담고 컨트롤러까지 전달
```

또한 `BindingResult`는 모델에 자동으로 포함되므로 Thymeleaf가 검증 결과를 사용할 수 있다.

---

## 3. 필드 오류와 객체 오류

### `FieldError`

특정 프로퍼티에 연결되는 오류다.

```java
bindingResult.addError(
    new FieldError(
        "item",
        "itemName",
        "상품명을 입력해야 합니다."
    )
);
```

```text
objectName
→ 폼 객체 이름

field
→ 오류가 발생한 프로퍼티

defaultMessage
→ 별도 메시지를 찾지 못했을 때 사용할 문구
```

### `ObjectError`

여러 필드가 함께 관련된 규칙에는 객체 오류를 사용한다.

```java
bindingResult.addError(
    new ObjectError(
        "item",
        "주문 총액 조건을 충족하지 못했습니다."
    )
);
```

```text
한 필드만 잘못됨
→ FieldError

여러 값의 조합이 잘못됨
→ ObjectError
```

---

## 4. Thymeleaf에서 오류 표시하기

`BindingResult`가 있으면 Thymeleaf의 검증 편의 기능을 사용할 수 있다.

```html
<form th:object="${item}">
    <input
        type="text"
        th:field="*{itemName}"
        th:errorclass="field-error">

    <div
        class="field-error"
        th:errors="*{itemName}">
    </div>
</form>
```

### 주요 기능

| 기능 | 역할 |
|---|---|
| `#fields.hasGlobalErrors()` | 객체 전체 오류 존재 여부 확인 |
| `#fields.globalErrors()` | 객체 전체 오류 목록 조회 |
| `th:errors` | 해당 필드의 오류 메시지 출력 |
| `th:errorclass` | 오류가 있을 때 CSS 클래스 추가 |

글로벌 오류는 다음처럼 출력할 수 있다.

```html
<div th:if="${#fields.hasGlobalErrors()}">
    <p
        class="field-error"
        th:each="error : ${#fields.globalErrors()}"
        th:text="${error}">
    </p>
</div>
```

직접 만든 `Map`을 조회하는 조건문보다 폼 필드와 오류가 같은 구조로 연결된다.

---

## 5. 사용자가 입력한 값 유지하기

검증에 실패했을 때 사용자가 입력한 값이 사라지면 다시 입력해야 한다.

`FieldError`의 상세 생성자는 거절된 값을 저장할 수 있다.

```java
bindingResult.addError(
    new FieldError(
        "item",
        "price",
        item.getPrice(),
        false,
        null,
        null,
        "가격을 확인해 주세요."
    )
);
```

중요한 값은 다음과 같다.

```text
rejectedValue
→ 검증에서 거절된 사용자 입력값

bindingFailure
→ 타입 변환 자체가 실패했는지 여부
```

`th:field`는 정상 상태에서는 모델 객체의 값을 사용하고, 오류가 있으면 `FieldError`가 보관한 값을 참고해 화면을 다시 만든다.

타입 변환이 실패한 경우에는 Spring이 원래 입력값을 `FieldError`에 보관한다. 예를 들어 `"abc"`를 `Integer`로 바꾸지 못하더라도 사용자가 쓴 문자열을 오류 화면에 다시 보여 줄 수 있다.

---

## 6. 오류 문구를 코드 밖으로 분리하기

컨트롤러에 오류 문장을 직접 쓰면 같은 규칙을 여러 곳에서 재사용하기 어렵다.

`errors.properties`에 메시지를 둘 수 있다.

```properties
required.item.itemName=상품명을 입력해 주세요.
range.item.price=가격은 {0}원부터 {1}원 사이여야 합니다.
max.item.quantity=수량은 {0}개를 넘을 수 없습니다.
totalPriceMin=총액은 {0}원 이상이어야 합니다. 현재 금액은 {1}원입니다.
```

Spring Boot가 메시지 파일을 읽도록 기준 이름을 추가한다.

```properties
spring.messages.basename=messages,errors
```

오류 객체에는 메시지 코드와 치환 인자를 넣는다.

```java
bindingResult.addError(
    new FieldError(
        "item",
        "price",
        item.getPrice(),
        false,
        new String[]{"range.item.price"},
        new Object[]{1000, 1000000},
        null
    )
);
```

이 방식은 동작하지만 `FieldError` 생성자에 전달할 값이 많아 코드가 길어진다.

---

## 7. `rejectValue()`와 `reject()`

`BindingResult`는 자신이 어떤 객체를 검증하는지 알고 있으므로 오류 객체를 직접 만들지 않고도 등록할 수 있다.

### 필드 오류

```java
bindingResult.rejectValue(
    "price",
    "range",
    new Object[]{1000, 1000000},
    null
);
```

### 객체 오류

```java
bindingResult.reject(
    "totalPriceMin",
    new Object[]{10000, total},
    null
);
```

```text
rejectValue()
→ 특정 필드 오류 등록

reject()
→ 객체 전체 오류 등록
```

여기서 `"range"`는 최종 메시지 문자열이 아니라 메시지 코드를 만드는 기준값이다.

---

## 8. 오류 코드가 여러 단계로 만들어지는 이유

하나의 오류 코드만 사용하면 모든 필드에 같은 메시지를 적용하거나 모든 경우를 따로 작성해야 한다.

Spring은 구체적인 코드부터 범용 코드까지 여러 후보를 만든다.

예를 들어 `item.price`에 `range` 오류를 등록하면 다음과 같은 후보가 생성될 수 있다.

```text
range.item.price
range.price
range.java.lang.Integer
range
```

메시지 조회는 위에서 아래 순서로 진행된다.

```text
가장 구체적인 메시지 존재
→ 해당 문구 사용

구체적인 메시지 없음
→ 더 범용적인 코드 조회

모두 없음
→ 기본 메시지 사용
```

이 구조 덕분에 기본 오류 문구는 공통으로 사용하고, 특정 화면이나 필드만 별도 문구로 덮어쓸 수 있다.

---

## 9. `MessageCodesResolver`

오류 코드 후보를 만드는 역할은 `MessageCodesResolver`가 담당한다.

기본 구현체는 객체명, 필드명, 필드 타입을 조합해 우선순위가 있는 코드 목록을 만든다.

```text
ObjectError
→ code.objectName
→ code

FieldError
→ code.objectName.field
→ code.field
→ code.fieldType
→ code
```

`reject()`와 `rejectValue()`는 내부적으로 이 규칙을 사용한다.

Thymeleaf의 `th:errors`는 오류 객체가 가진 코드들을 순서대로 메시지 소스에서 찾고, 가장 먼저 발견한 문구를 출력한다.

---

## 10. 타입 오류 메시지 바꾸기

가격에 문자를 입력하면 Spring이 바인딩 단계에서 `typeMismatch` 오류를 만든다.

```text
typeMismatch.item.price
typeMismatch.price
typeMismatch.java.lang.Integer
typeMismatch
```

기본 오류 문구는 사용자에게 지나치게 기술적으로 보일 수 있다.

`errors.properties`에 사용자용 문구를 추가한다.

```properties
typeMismatch.java.lang.Integer=숫자를 입력해 주세요.
typeMismatch=입력 형식을 확인해 주세요.
```

바인딩 오류는 개발자가 조건문으로 추가하지 않아도 Spring이 먼저 등록한다.

직접 검증에서도 같은 필드에 오류를 하나 더 추가하면 메시지가 중복될 수 있으므로, 이미 바인딩 오류가 있는 필드는 별도 검증을 건너뛰는 방식도 고려해야 한다.

---

## 11. 검증기 분리

컨트롤러에서 검증 조건이 커지면 별도 클래스로 옮길 수 있다.

```java
@Component
public class ItemValidator implements Validator {

    @Override
    public boolean supports(Class<?> type) {
        return Item.class.isAssignableFrom(type);
    }

    @Override
    public void validate(
            Object target,
            Errors errors
    ) {
        Item item = (Item) target;

        if (item.getPrice() == null
                || item.getPrice() < 1000
                || item.getPrice() > 1000000) {
            errors.rejectValue(
                "price",
                "range",
                new Object[]{1000, 1000000},
                null
            );
        }

        if (item.getQuantity() == null
                || item.getQuantity() > 9999) {
            errors.rejectValue(
                "quantity",
                "max",
                new Object[]{9999},
                null
            );
        }
    }
}
```

### `supports()`

현재 검증기가 해당 타입을 처리할 수 있는지 판단한다.

### `validate()`

실제 조건을 검사하고 `Errors`에 결과를 등록한다.

컨트롤러에서 직접 호출할 수도 있다.

```java
itemValidator.validate(item, bindingResult);
```

검증 규칙이 컨트롤러 밖으로 이동해 요청 처리와 검증 책임이 분리된다.

---

## 12. `WebDataBinder`와 자동 검증

컨트롤러에 검증기를 등록하면 직접 호출하지 않고도 검증을 실행할 수 있다.

```java
@InitBinder
public void init(WebDataBinder binder) {
    binder.addValidators(itemValidator);
}
```

검증 대상에 `@Validated`를 붙인다.

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

처리 흐름:

```text
요청 파라미터 바인딩
→ @Validated 확인
→ 등록된 Validator 탐색
→ supports()로 대상 타입 확인
→ validate() 실행
→ BindingResult에 오류 저장
```

`@InitBinder`에 등록한 설정은 해당 컨트롤러 범위에서 적용된다.

---

## 13. 내가 구분해서 기억할 기준

| 상황 | 사용 방식 |
|---|---|
| 작은 예제에서 빠르게 오류 확인 | 직접 조건문과 Map |
| Spring MVC 폼 오류와 연결 | `BindingResult` |
| 특정 필드 오류 | `rejectValue()` |
| 여러 필드의 조합 오류 | `reject()` |
| 사용자 입력값 유지 | `FieldError.rejectedValue` |
| 공통 오류 문구 관리 | `errors.properties` |
| 오류 코드 우선순위 | `MessageCodesResolver` |
| 검증 로직 재사용 | `Validator` 구현 |
| 컨트롤러에서 자동 실행 | `WebDataBinder` + `@Validated` |

## 핵심 정리

- 검증은 바인딩된 값을 저장하기 전에 규칙에 맞는지 확인하는 과정이다.
- `BindingResult`는 바인딩 오류와 검증 오류를 한곳에 보관한다.
- `FieldError`는 필드 오류, `ObjectError`는 객체 전체 오류를 표현한다.
- Thymeleaf는 `th:errors`, `th:errorclass`, `#fields`로 오류 결과를 화면에 연결한다.
- `rejectValue()`와 `reject()`를 사용하면 오류 객체 생성 코드를 줄일 수 있다.
- 오류 코드는 구체적인 형태부터 범용 형태까지 생성되어 메시지를 단계적으로 선택한다.
- 타입 변환 오류도 `BindingResult`에 들어오며 메시지 코드를 재정의할 수 있다.
- 검증 코드가 커지면 `Validator`로 분리하고 `WebDataBinder`에 등록할 수 있다.

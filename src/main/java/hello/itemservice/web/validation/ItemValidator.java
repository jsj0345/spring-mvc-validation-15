package hello.itemservice.web.validation;

import hello.itemservice.domain.item.Item;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ItemValidator implements Validator {

  @Override
  public boolean supports(Class<?> clazz) {
    return Item.class.isAssignableFrom(clazz);
    // item == clazz (클래스가 서로 같은지 참조값 비교x)
    // item == subItem (자식 클래스여도 통과 다형성 개념)
  }

  @Override
  public void validate(Object target, Errors errors) {
    Item item = (Item) target;

    // 검증 로직
    if(!StringUtils.hasText(item.getItemName())) {
      errors.rejectValue("itemName", "required"); // 에러코드의 맨 앞 부분만 작성. (required)뒤에 objectName, field가 붙는 형식.
    }

    if(item.getPrice() == null || item.getPrice() < 1000 || item.getPrice() > 1000000) {
      errors.rejectValue("price", "range", new Object[]{1000, 1000000}, null);
    }

    if(item.getQuantity() == null || item.getQuantity() >= 9999) {
      errors.rejectValue("quantity", "max", new Object[]{9999}, null);
    }

    // 특정 필드가 아닌 복합 룰 검증
    if(item.getPrice() != null && item.getQuantity() != null) {
      int resultPrice = item.getPrice() * item.getQuantity();
      if(resultPrice < 10000) {
        errors.reject("totalPriceMin", new Object[]{1000, resultPrice}, null);
      }
    }

  }

}

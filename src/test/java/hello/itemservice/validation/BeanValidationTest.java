package hello.itemservice.validation;

import hello.itemservice.domain.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Locale;
import java.util.Set;

public class BeanValidationTest {

  @BeforeAll
  static void setLocale() {
    Locale.setDefault(Locale.KOREA); // Bean Validation 기본 메시지를 한국어로 출력하기 위해 JVM 기본 Locale을 한국으로 설정.
  }


  @Test
  void beanValidation() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator(); // 공장에서 실제 검증기를 갖고 온 것.

    Item item = new Item();
    item.setItemName(" "); // 공백
    item.setPrice(0);
    item.setQuantity(10000);

    Set<ConstraintViolation<Item>> violations = validator.validate(item);

    for(ConstraintViolation<Item> violation : violations) {
      System.out.println("violation = " + violation);
      System.out.println("violation = " + violation.getMessage());
    }
  }

}



package hello.itemservice.validation;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.MessageCodesResolver;

import java.util.Arrays;

public class MessageCodesResolverTest {

  MessageCodesResolver codesResolver = new DefaultMessageCodesResolver();

  @Test
  void messageCodesResolverObject() {
    String[] messageCodes = codesResolver.resolveMessageCodes("required", "item");

//    for (String messageCode : messageCodes) {
//      System.out.println("messageCode = " + messageCode);
//    }

    System.out.println(Arrays.toString(messageCodes));

    assertThat(messageCodes).containsExactly("required.item", "required");
  }

  @Test
  void messageCodesResolverField() {
    String[] messageCodes = codesResolver.resolveMessageCodes("required", "item", "itemName", String.class);
    for (String messageCode : messageCodes) {
      System.out.println("messageCode = " + messageCode);
    }
    //bindingResult.rejectValue("itemName", "required");
    /*
    rejectValue 메서드에서 resolveMessageCodes를 사용함.
    */

    System.out.println(Arrays.toString(messageCodes));

    assertThat(messageCodes).containsExactly("required.item.itemName", "required.itemName", "required.java.lang.String", "required");


  }
}

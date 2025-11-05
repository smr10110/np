package cl.ufo.dcl.cryptapp.service;

import cl.ufo.dcl.cryptapp.dto.MessageDTO;
import cl.ufo.dcl.cryptapp.model.Message;
import cl.ufo.dcl.cryptapp.repository.MessageRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AssertionErrors;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MessageServiceTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;


    @Test
    @DisplayName("Save new encrypted message successfully")
    @Order(1)
    public void testSuccessfulMessageSave(){

        //given
        String plainText = "mi texto sin cifrar";
        String key = "123456789asdfghj";
        MessageDTO messageDTO = MessageDTO.builder().
                msgDtoPlainText(plainText).
                msgDtoKey(key).
                build();
        String expected = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";


        //when
        Message savedMessage = messageService.save(messageDTO);

        //then
        AssertionErrors.assertNotNull("Message should not be null", savedMessage);
        Assertions.assertEquals(expected, savedMessage.getMesCipherText());
        Assertions.assertEquals(1L, savedMessage.getMesId());
    }


    @Test
    @DisplayName("Get decrpypt message by Id")
    @Order(2)
    void findById() {

        // given
        String key = "123456789asdfghj";
        String cipherText = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";
        String expected = "mi texto sin cifrar";

        //when
        MessageDTO messageDTO = MessageDTO.builder().
                msgDtoId(1L).
                msgDtoKey(key).
                build();

        MessageDTO retrievedMessage = messageService.findById(messageDTO);

        //then
        AssertionErrors.assertNotNull("Message should not be null", retrievedMessage);
        Assertions.assertEquals(expected, retrievedMessage.getMsgDtoPlainText());
        Assertions.assertEquals(1L, retrievedMessage.getMsgDtoId());
    }
}
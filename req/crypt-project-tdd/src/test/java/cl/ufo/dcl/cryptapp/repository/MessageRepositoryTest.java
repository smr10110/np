package cl.ufo.dcl.cryptapp.repository;

import cl.ufo.dcl.cryptapp.model.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;


@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @AfterEach
    void tearDown() {
        messageRepository.deleteAll();
    }

    @Test
    @DisplayName("Message saved successfully")
    public void testMessageSavedSuccessfully() {
        //given
        String newCipherText = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";
        Message newMessage = Message.builder().mesCipherText(newCipherText).build();

        //when
        Message savedMessage = messageRepository.save(newMessage);

        //then
        Assertions.assertNotNull(savedMessage, "Message should be saved");
        Assertions.assertNotNull(savedMessage.getMesId(), "Message should have an id when saved");
        Assertions.assertEquals(newMessage.getMesCipherText(), savedMessage.getMesCipherText());
    }

    @Test
    @Sql("/fixture/data.sql")
    @DisplayName("Message with id retrieved succesfuly")
    public void testMessageWithRetrievedSuccessfully() {
        //given one message in the database
        String expected = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";

        //when
        Message retrievedMessage = messageRepository.findMessageByMesId(1L);

        //then
        Assertions.assertNotNull(retrievedMessage, "Message with id 1 should exist");
        Assertions.assertEquals(expected, retrievedMessage.getMesCipherText());
    }

    @Test
    @Sql("/fixture/data.sql")
    @DisplayName("Message not found with non-existint id")
    public void testMessageNotFoundNonExistingId(){
        //Given
        //2 message in the  database
        Long mesId = 4L;

        //when
        Message retrievedMessage = messageRepository.findMessageByMesId(mesId);

        //then
        Assertions.assertNull(retrievedMessage, "Message with mesId should not exist");
    }
}
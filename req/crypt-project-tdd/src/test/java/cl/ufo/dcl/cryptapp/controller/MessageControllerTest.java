package cl.ufo.dcl.cryptapp.controller;

import cl.ufo.dcl.cryptapp.dto.MessageDTO;
import cl.ufo.dcl.cryptapp.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    @DisplayName("crypt and save new messaage - POST /message/cifrar")
    public void testAddNewMessage() throws Exception {
        // Give
        String plainText = "mi texto sin cifrar";
        String key = "123456789asdfghj";
        String cryptText = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";

        MessageDTO messageDTO = MessageDTO.builder().msgDtoPlainText(plainText).msgDtoKey(key).build();

        //when that Perform POST request
        mockMvc.perform(post("/message/cifrar").contentType(MediaType.APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(messageDTO)))

                //Then

                // Validate 201 CREATED and JSON response type received
                .andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

                // Validate response headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\"")).andExpect(header().string(HttpHeaders.LOCATION, "/message/1"))

                // Validate response body
                .andExpect(jsonPath("$.mesId", is(1))).andExpect(jsonPath("$.mesCipherText", is(cryptText)));
    }


    @Test
    @DisplayName("get and decrypt messaage - POST /message/decifrar")
    public void testGetMessageById() throws Exception {
        // Give
        String plainText = "mi texto sin cifrar";
        String key = "123456789asdfghj";
        String cryptText = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";

        MessageDTO messageDTO = MessageDTO.builder().msgDtoId(1L).msgDtoKey(key).build();

        //when that Perform POST request
        mockMvc.perform(post("/message/decifrar").contentType(MediaType.APPLICATION_JSON_VALUE).content(new ObjectMapper().writeValueAsString(messageDTO)))

                //Then

                // Validate 201 CREATED and JSON response type received
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))

                // Validate response headers
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\"")).andExpect(header().string(HttpHeaders.LOCATION, "/message/1"))

                // Validate response body
                //.andExpect(jsonPath("$.msgDtoId", is(1)))
                .andExpect(jsonPath("$.msgDtoPlainText", is(plainText)));
    }

}
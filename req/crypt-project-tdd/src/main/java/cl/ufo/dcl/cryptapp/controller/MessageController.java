package cl.ufo.dcl.cryptapp.controller;

import cl.ufo.dcl.cryptapp.dto.MessageDTO;
import cl.ufo.dcl.cryptapp.model.Message;
import cl.ufo.dcl.cryptapp.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     *
     * @param messageDTO
     * @return
     */
    @PostMapping(value = "/cifrar", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> crypt(@RequestBody MessageDTO messageDTO) {
        Log.info("" + messageDTO);

        Message newMessage = messageService.save(messageDTO);
        try {
            return ResponseEntity
                    .created(new URI("/message/" + newMessage.getMesId()))
                    .eTag(newMessage.getMesId().toString())
                    .body(newMessage);
        } catch (URISyntaxException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostMapping(value = "/decifrar", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> decrypt(@RequestBody MessageDTO messageDTO) {

        MessageDTO retrievedMessage = messageService.findById(messageDTO);
        Log.info("" + retrievedMessage);


        if (retrievedMessage != null) { //we have to fix this ...thing with optional
            try {

                return ResponseEntity
                        .ok()
                        .eTag(retrievedMessage.getMsgDtoId().toString())
                        .location(new URI("/message/" + retrievedMessage.getMsgDtoId()))
                        .body(retrievedMessage);
            } catch (URISyntaxException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private static final Logger Log = Logger.getLogger(MessageController.class.getName());
}

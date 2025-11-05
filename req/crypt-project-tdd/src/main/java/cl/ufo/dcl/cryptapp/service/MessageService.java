package cl.ufo.dcl.cryptapp.service;

import cl.ufo.dcl.cryptapp.dto.MessageDTO;
import cl.ufo.dcl.cryptapp.model.Message;
import cl.ufo.dcl.cryptapp.repository.MessageRepository;
import cl.ufo.dcl.cryptapp.utils.AESCrypt;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class MessageService {

    private static final Logger Log = Logger.getLogger(MessageService.class.getName());

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message save(MessageDTO messageDto) {
        Log.info("Saving new message with text: " + messageDto);

        AESCrypt cfr = new AESCrypt(messageDto.getMsgDtoPlainText(),
                messageDto.getMsgDtoKey());

        String cryptText = cfr.encrypt();
        Log.info("LOG: " + cryptText);
        Message messageToSave = Message.builder().
                mesCipherText(cryptText).
                build();

        Log.info("LOG: " + messageToSave);

        return messageRepository.save(messageToSave);
    }


    public MessageDTO findById(MessageDTO messageDto) {
        Log.info("Message before " + messageDto);
        Message message = messageRepository.findMessageByMesId(messageDto.getMsgDtoId());
        Log.info("Message: " + message);
        AESCrypt cfr = new AESCrypt(message.getMesCipherText(),
                messageDto.getMsgDtoKey());

        messageDto.setMsgDtoPlainText(cfr.decrypt());
        messageDto.setMsgDtoCipherText(message.getMesCipherText());
        Log.info("Message after " + messageDto);
        return messageDto;
    }
}

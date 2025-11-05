package cl.ufo.dcl.cryptapp.repository;

import cl.ufo.dcl.cryptapp.model.Message;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends CrudRepository<Message, Long> {
    Message findMessageByMesId(Long id);
}

package cl.ufo.dcl.cryptapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    private Long msgDtoId;
    private String msgDtoKey;
    private String msgDtoCipherText;
    private String msgDtoPlainText;
}

package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResultadosRequestDTO {
    private int minutosConsumidos;
    private int segundosConsumidos;
    private List<RespuestaDTO> respuestas;
}

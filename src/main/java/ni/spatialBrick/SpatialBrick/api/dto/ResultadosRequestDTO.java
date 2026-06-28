package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.DuracionMinutos;
import java.util.List;

@Data
public class ResultadosRequestDTO {
    private DuracionMinutos tiempoConsumido;
    private List<RespuestaDTO> respuestas;
}

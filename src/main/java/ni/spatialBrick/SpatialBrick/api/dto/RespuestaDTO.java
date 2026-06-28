package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.OpcionRespuesta;

@Data
public class RespuestaDTO {
    private int numeroEjercicio;
    private OpcionRespuesta opcionElegida;
}

package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ResultadosResponseDTO {
    private int idIntento;
    private int cantidadAciertos;
    private int percentil;
    private BigDecimal puntuacionTotal;
    private String mensaje;
}

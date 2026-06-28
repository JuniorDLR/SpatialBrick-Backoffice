package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class TestResponseDTO {
    private String codigoTest;
    private String instrucciones;
    private int tiempoLimiteSegundos;
    private List<EjercicioDTO> ejercicios;
}

package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;

@Data
public class CandidatoTestStatusDTO {
    private String codigoTest;
    private String nombreTest;
    private String estado;
    private Integer idIntento;
}

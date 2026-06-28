package ni.spatialBrick.SpatialBrick.api.dto;

import lombok.Data;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.Genero;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.NivelEducativo;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.Puesto;
import java.util.Date;

@Data
public class CandidatoRequestDTO {
    private String identificacion;
    private String nombreCompleto;
    private NivelEducativo nivelEducativo;
    private Genero genero;
    private Date fechaNacimiento;
    private String email;
    private String telefono;
    private Puesto puestoAplica;
    private String profesion;
    private String codigoTest;
}

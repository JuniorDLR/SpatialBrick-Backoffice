package ni.spatialBrick.SpatialBrick.modelo.configuracion;

import javax.persistence.*;
import org.openxava.annotations.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Table(name = "baremo_ladrillos_cubos", indexes = {
    @Index(name = "idx_baremo_puntuacion", columnList = "puntuacionMinima, puntuacionMaxima")
})
@Tab(properties = "percentil, puntuacionMinima, puntuacionMaxima")
@Getter @Setter
public class BaremoLadrillosCubos {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Hidden
    int id;

    int percentil;

    int puntuacionMinima;

    int puntuacionMaxima;
}

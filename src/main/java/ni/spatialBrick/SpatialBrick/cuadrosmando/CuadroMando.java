package ni.spatialBrick.SpatialBrick.cuadrosmando;

import org.openxava.annotations.*;
import org.openxava.jpa.XPersistence;

@View(members = 
    "candidatosTotales, intentosTotales"
)
public class CuadroMando {

    @LargeDisplay(icon="account-group")
    public int getCandidatosTotales() {
        return ((Long) XPersistence.getManager().createQuery("select count(c) from Candidato c").getSingleResult()).intValue();
    }

    @LargeDisplay(icon="checkbox-marked-circle-outline")
    public int getIntentosTotales() {
        return ((Long) XPersistence.getManager().createQuery("select count(i) from IntentoTest i").getSingleResult()).intValue();
    }
}

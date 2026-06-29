package ni.spatialBrick.SpatialBrick.acciones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openxava.actions.OnChangePropertyBaseAction;
import org.openxava.jpa.XPersistence;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.TestLadrillosCubos;

public class CargarRespuestasDeTestAccion extends OnChangePropertyBaseAction {

    @Override
    public void execute() throws Exception {
        Object testId = getNewValue();
        if (testId == null) {
            getView().getSubview("respuestas").getCollectionValues().clear();
            return;
        }

        TestLadrillosCubos test = XPersistence.getManager().find(TestLadrillosCubos.class, testId);
        if (test != null && test.getEjercicios() != null) {
            List<Map<String, Object>> nuevasRespuestas = new ArrayList<>();
            for (ni.spatialBrick.SpatialBrick.modelo.configuracion.EjercicioCubos ej : test.getEjercicios()) {
                Map<String, Object> fila = new HashMap<>();
                
                Map<String, Object> ejercicioKey = new HashMap<>();
                ejercicioKey.put("id", ej.getId());
                ejercicioKey.put("imagenMonton", ej.getImagenMonton());
                fila.put("ejercicio", ejercicioKey);
                
                nuevasRespuestas.add(fila);
            }
            getView().setValue("respuestas", nuevasRespuestas);
        }
    }
}

package ni.spatialBrick.SpatialBrick.acciones;

import ni.spatialBrick.SpatialBrick.modelo.transaccional.IntentoTest;
import org.openxava.actions.SaveAction;
import org.openxava.jpa.XPersistence;

public class GrabarIntentoAccion extends SaveAction {

    @Override
    public void execute() throws Exception {
        setResetAfter(false);
        super.execute();

        if (getErrors().isEmpty()) {
            calcularYActualizarResultados();
            addMessage("Resultados calculados y guardados exitosamente.");
        }
    }

    private void calcularYActualizarResultados() {
        Object id = getView().getValue("id");
        if (id == null) return;

        IntentoTest intento = XPersistence.getManager().find(IntentoTest.class, id);
        if (intento == null) return;

        intento.calcularPuntuacionFinal();
        XPersistence.getManager().merge(intento);
        actualizarVistaConResultados(intento);
    }

    private void actualizarVistaConResultados(IntentoTest intento) {
        getView().setValue("puntuacionTotal", intento.getPuntuacionTotal());
        getView().setValue("cantidadAciertos", intento.getCantidadAciertos());
        getView().setValue("cantidadErrores", intento.getCantidadErrores());
        getView().setValue("cantidadOmisiones", intento.getCantidadOmisiones());
        getView().setValue("percentil", intento.getPercentil());
    }
}

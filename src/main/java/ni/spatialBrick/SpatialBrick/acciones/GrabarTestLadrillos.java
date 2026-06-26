package ni.spatialBrick.SpatialBrick.acciones;

import org.openxava.actions.SaveAction;

public class GrabarTestLadrillos extends SaveAction {
    @Override
    public void execute() throws Exception {
        boolean esNuevo = getView().isKeyEditable();
        super.execute();
        getMessages().removeAll();
        if (esNuevo) {
            addMessage("Test configurado y creado exitosamente.");
        } else {
            addMessage("Configuración del test actualizada exitosamente.");
        }
    }
}

package ni.spatialBrick.SpatialBrick.acciones;

import org.openxava.actions.DeleteAction;

public class EliminarTestLadrillos extends DeleteAction {
    @Override
    public void execute() throws Exception {
        super.execute();
        getMessages().removeAll();
        addMessage("Test Ladrillos Cubos eliminado exitosamente.");
    }
}

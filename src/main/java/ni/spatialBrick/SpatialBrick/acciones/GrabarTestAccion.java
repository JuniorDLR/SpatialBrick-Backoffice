package ni.spatialBrick.SpatialBrick.acciones;

import org.openxava.actions.SaveAction;
import org.openxava.web.editors.AttachedFile;
import org.openxava.web.editors.FilePersistorFactory;
import org.openxava.web.editors.IFilePersistor;
import org.apache.tika.Tika;
import java.util.List;
import java.util.Map;

public class GrabarTestAccion extends SaveAction {

    @Override
    public void execute() throws Exception {
        if (!validarImagenes()) {
            return;
        }
        super.execute();
    }

    private boolean validarImagenes() {

        List<Map<String, Object>> ejercicios;
        try {
            ejercicios = getView().getSubview("ejercicios").getCollectionValues();
        } catch (Exception e) {
            return true;
        }

        if (ejercicios == null || ejercicios.isEmpty()) return true;

        int numero = 1;
        Tika tika = new Tika();
        IFilePersistor persistor = FilePersistorFactory.getInstance();

        for (Map<String, Object> row : ejercicios) {
            String fileId = (String) row.get("imagenMonton");

            if (fileId == null || fileId.trim().isEmpty()) {
                addError("Ejercicio #" + numero + ": Debe subir una imagen válida (JPG o PNG). Si subió un archivo, verifique que sea una imagen real y no otro tipo de archivo.");
                return false;
            }

            try {
                AttachedFile file = persistor.find(fileId);
                if (file == null || file.getData() == null) {
                    addError("Ejercicio #" + numero + ": No se pudo localizar la imagen subida.");
                    return false;
                }

                byte[] imagen = file.getData();
                String mimeType = tika.detect(imagen);
                
                if (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png")) {
                    addError("Ejercicio #" + numero + ": Alerta de Seguridad — El archivo subido fue detectado como '" + mimeType + "'. Solo se permiten formatos JPG o PNG reales.");
                    return false;
                }
            } catch (Exception e) {
                addError("Ejercicio #" + numero + ": Error al leer el archivo. " + e.getMessage());
                return false;
            }
            numero++;
        }
        return true;
    }
}

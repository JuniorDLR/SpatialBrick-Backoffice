package ni.spatialBrick.SpatialBrick.run;

import org.openxava.util.*;

/**
 * Ejecuta esta clase para arrancar la aplicación.
 */

public class SpatialBrick {

	public static void main(String[] args) throws Exception {
		// DBServer.start("SpatialBrick-db"); // Comentado para usar PostgreSQL
		AppServer.run("SpatialBrick"); // Usa AppServer.run("") para funcionar en el contexto raíz
	}

}

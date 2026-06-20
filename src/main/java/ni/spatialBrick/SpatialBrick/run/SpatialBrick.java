package ni.spatialBrick.SpatialBrick.run;

import org.openxava.util.*;

/**
 * Ejecuta esta clase para arrancar la aplicación.
 */

public class SpatialBrick {

	public static void main(String[] args) throws Exception {
		DBServer.start("SpatialBrick-db"); // Para usar tu propia base de datos comenta esta línea y configura src/main/webapp/META-INF/context.xml
		AppServer.run("SpatialBrick"); // Usa AppServer.run("") para funcionar en el contexto raíz
	}

}

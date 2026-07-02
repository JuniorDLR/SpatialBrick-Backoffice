package ni.spatialBrick.SpatialBrick.listeners;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.persistence.EntityManager;
import org.openxava.jpa.XPersistence;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.BaremoLadrillosCubos;

@WebListener
public class InicializadorBaremos implements ServletContextListener {

    private static final int PUNTUACION_MAXIMA_POSIBLE = 27;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        EntityManager em = null;
        try {
            em = XPersistence.createManager();
            Long count = (Long) em.createQuery("select count(b) from BaremoLadrillosCubos b").getSingleResult();

            if (count == 0) {
                em.getTransaction().begin();
                cargarBaremosNacionales(em);
                em.getTransaction().commit();
                System.out.println("====== [SpatialBrick] Tabla BaremoLadrillosCubos cargada exitosamente con las normas Nacionales BFA ======");
            }
        } catch (Exception e) {
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            System.err.println("Error inicializando BaremoLadrillosCubos: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private void cargarBaremosNacionales(EntityManager em) {
        insertarBaremo(em, 99, 27, PUNTUACION_MAXIMA_POSIBLE);
        insertarBaremo(em, 97, 26, 26);
        insertarBaremo(em, 95, 25, 25);
        insertarBaremo(em, 90, 24, 24);
        insertarBaremo(em, 85, 22, 23);
        insertarBaremo(em, 80, 20, 21);
        insertarBaremo(em, 75, 19, 19);
        insertarBaremo(em, 70, 18, 18);
        insertarBaremo(em, 60, 17, 17);
        insertarBaremo(em, 55, 16, 16);
        insertarBaremo(em, 50, 15, 15);
        insertarBaremo(em, 40, 14, 14);
        insertarBaremo(em, 30, 13, 13);
        insertarBaremo(em, 25, 12, 12);
        insertarBaremo(em, 20, 11, 11);
        insertarBaremo(em, 15, 10, 10);
        insertarBaremo(em, 10, 9, 9);
        insertarBaremo(em, 5, 8, 8);
        insertarBaremo(em, 1, 0, 7);
    }

    private void insertarBaremo(EntityManager em, int percentil, int min, int max) {
        BaremoLadrillosCubos baremo = new BaremoLadrillosCubos();
        baremo.setPercentil(percentil);
        baremo.setPuntuacionMinima(min);
        baremo.setPuntuacionMaxima(max);
        em.persist(baremo);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Sin acciones necesarias al destruir el contexto
    }
}

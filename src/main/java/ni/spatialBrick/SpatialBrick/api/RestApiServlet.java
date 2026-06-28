package ni.spatialBrick.SpatialBrick.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ni.spatialBrick.SpatialBrick.api.dto.*;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.EjercicioCubos;
import ni.spatialBrick.SpatialBrick.modelo.configuracion.TestLadrillosCubos;
import ni.spatialBrick.SpatialBrick.modelo.enumeraciones.EstadoIntento;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.Candidato;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.IntentoTest;
import ni.spatialBrick.SpatialBrick.modelo.transaccional.RespuestaCandidato;
import org.openxava.jpa.XPersistence;
import org.openxava.web.editors.AttachedFile;
import org.openxava.web.editors.FilePersistorFactory;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet central que expone la API REST del sistema SpatialBrick.
 * Usa HttpServlet nativo en lugar de JAX-RS porque OpenXava deshabilita
 * el escaneo de anotaciones (JarScanner scanClassPath="false") y no
 * incluye un runtime JAX-RS (CXF/Jersey) como dependencia.
 * Rutas soportadas:
 *   POST /rest/candidatos/iniciar-test     -> Registrar candidato e iniciar test
 *   GET  /rest/test/{codigo}               -> Obtener estructura del test
 *   GET  /rest/test/imagen/{id}            -> Descargar imagen de ejercicio
 *   POST /rest/intentos/{id}/finalizar     -> Finalizar y calificar un intento
 */
public class RestApiServlet extends HttpServlet {

    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // ======================== DISPATCHER ========================

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        configurarCORS(resp);
        String method = req.getMethod();
        
        if ("OPTIONS".equalsIgnoreCase(method)) {
            resp.setStatus(200);
            return;
        }

        String path = req.getPathInfo();
        if (path != null && (path.contains("..") || path.contains("./"))) {
            enviarError(resp, 400, "Ruta inválida.");
            return;
        }
        
        try {
            if ("GET".equalsIgnoreCase(method)) {
                doGet(req, resp);
            } else if ("POST".equalsIgnoreCase(method)) {
                doPost(req, resp);
            } else {
                enviarError(resp, 405, "Método no permitido.");
            }
        } catch (Exception e) {
            enviarError(resp, 500, "Error interno: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        
        if (path.startsWith("/test/imagen/")) {
            String idImagen = path.substring("/test/imagen/".length());
            descargarImagen(idImagen, resp);
        } else if (path.startsWith("/test/")) {
            String codigo = path.substring("/test/".length());
            obtenerEstructuraTest(codigo, resp);
        } else {
            enviarError(resp, 404, "Ruta no encontrada: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        
        if (path.equals("/candidatos/iniciar-test")) {
            CandidatoRequestDTO request = leerJsonBody(req, CandidatoRequestDTO.class);
            iniciarTest(request, resp);
        } else if (path.matches("/intentos/\\d+/finalizar")) {
            String[] partes = path.split("/");
            int idIntento = Integer.parseInt(partes[2]);
            ResultadosRequestDTO request = leerJsonBody(req, ResultadosRequestDTO.class);
            finalizarTest(idIntento, request, resp);
        } else {
            enviarError(resp, 404, "Ruta no encontrada: " + path);
        }
    }

    // ======================== CANDIDATO ========================

    private void iniciarTest(CandidatoRequestDTO request, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        try {

            // 1. Buscar o Crear Candidato
            Candidato candidato = buscarCandidatoPorIdentificacion(em, request.getIdentificacion());
            if (candidato == null) {
                candidato = new Candidato();
                candidato.setIdentificacion(request.getIdentificacion());
            }

            // Actualizar datos del candidato con la información más reciente
            candidato.setNombreCompleto(request.getNombreCompleto());
            candidato.setNivelEducativo(request.getNivelEducativo());
            candidato.setGenero(request.getGenero());
            candidato.setFechaNacimiento(request.getFechaNacimiento());
            candidato.setEmail(request.getEmail());
            candidato.setTelefono(request.getTelefono());
            candidato.setPuestoAplica(request.getPuestoAplica());
            candidato.setProfesion(request.getProfesion());

            if (candidato.getId() == 0) {
                em.persist(candidato);
            } else {
                em.merge(candidato);
            }

            // 2. Validar que el Test exista
            TestLadrillosCubos test;
            try {
                test = (TestLadrillosCubos) em.createQuery(
                        "select t from TestLadrillosCubos t where t.codigoTest = :codigo")
                        .setParameter("codigo", request.getCodigoTest())
                        .getSingleResult();
            } catch (NoResultException e) {
                enviarError(resp, 404, "El test con código " + request.getCodigoTest() + " no existe.");
                return;
            }

            if (!test.isActivo()) {
                enviarError(resp, 403, "Este test está desactivado.");
                return;
            }

            // 3. Iniciar un nuevo Intento
            IntentoTest intento;
            try {
                intento = candidato.iniciarTest(test);
                em.persist(intento);
            } catch (IllegalStateException e) {
                enviarError(resp, 400, e.getMessage());
                return;
            }

            // 4. Devolver respuesta exitosa
            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("idIntento", intento.getId());
            responseData.put("mensaje", "Candidato registrado e intento de test iniciado correctamente.");

            resp.setStatus(201);
            enviarJson(resp, responseData);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().setRollbackOnly();
            }
            enviarError(resp, 400, e.getMessage() != null ? e.getMessage() : "Error interno al registrar el candidato.");
        }
    }

    // ======================== TEST ========================

    private void obtenerEstructuraTest(String codigoTest, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        try {
            TestLadrillosCubos test = (TestLadrillosCubos) em.createQuery(
                    "select t from TestLadrillosCubos t left join fetch t.ejercicios where t.codigoTest = :codigo")
                    .setParameter("codigo", codigoTest)
                    .getSingleResult();

            if (!test.isActivo()) {
                enviarError(resp, 403, "El test solicitado está desactivado.");
                return;
            }

            TestResponseDTO response = mapearTestADto(test);

            enviarJson(resp, response);

        } catch (NoResultException e) {
            enviarError(resp, 404, "Test no encontrado.");
        } catch (Exception e) {
            enviarError(resp, 500, "Error interno del servidor.");
        }
    }

    private void descargarImagen(String idImagen, HttpServletResponse resp) throws IOException {
        // Sanitizar entrada contra Path Traversal
        if (idImagen == null || idImagen.contains("/") || idImagen.contains("\\") || idImagen.contains("..")) {
            enviarError(resp, 400, "ID de imagen inválido.");
            return;
        }
        try {
            AttachedFile file = FilePersistorFactory.getInstance().find(idImagen);
            if (file == null || file.getData() == null) {
                enviarError(resp, 404, "Imagen no encontrada.");
                return;
            }

            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "inline; filename=\"" + file.getName() + "\"");
            resp.getOutputStream().write(file.getData());
            resp.getOutputStream().flush();

        } catch (Exception e) {
            enviarError(resp, 500, "Error al descargar imagen.");
        }
    }

    // ======================== INTENTO ========================

    private void finalizarTest(int idIntento, ResultadosRequestDTO request, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();

        try {

            IntentoTest intento = em.find(IntentoTest.class, idIntento);
            if (intento == null) {
                enviarError(resp, 404, "Intento no encontrado.");
                return;
            }

            if (intento.getEstado() == EstadoIntento.FINALIZADO) {
                enviarError(resp, 400, "Este intento ya fue finalizado y calificado previamente.");
                return;
            }

            // Inyectar el tiempo
            intento.setTiempoConsumido(request.getTiempoConsumido());

            // Inyectar las respuestas
            if (request.getRespuestas() != null) {
                for (RespuestaDTO resDto : request.getRespuestas()) {
                    RespuestaCandidato respuesta = new RespuestaCandidato();
                    respuesta.setNumeroEjercicio(resDto.getNumeroEjercicio());
                    respuesta.setOpcionElegida(resDto.getOpcionElegida());
                    intento.agregarRespuesta(respuesta);
                }
            }

            // Cambiar estado y calificar
            intento.setEstado(EstadoIntento.FINALIZADO);
            intento.calcularPuntuacionFinal();

            em.merge(intento);

            // Preparar respuesta para el frontend
            ResultadosResponseDTO response = mapearResultadosADto(intento);

            enviarJson(resp, response);

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().setRollbackOnly();
            }
            enviarError(resp, 500, "Error procesando el examen: " + e.getMessage());
        }
    }

    // ======================== UTILIDADES ========================

    private TestResponseDTO mapearTestADto(TestLadrillosCubos test) {
        TestResponseDTO response = new TestResponseDTO();
        response.setCodigoTest(test.getCodigoTest());
        response.setInstrucciones(test.getInstrucciones());
        response.setTiempoLimiteSegundos(test.getTiempoLimiteSegundos());

        List<EjercicioDTO> ejerciciosDTO = new ArrayList<>();
        if (test.getEjercicios() != null) {
            for (EjercicioCubos ej : test.getEjercicios()) {
                EjercicioDTO dto = new EjercicioDTO();
                dto.setNumeroEjercicio(ej.getNumeroEjercicio());
                dto.setImagenUrl("/rest/test/imagen/" + ej.getImagenMonton());
                ejerciciosDTO.add(dto);
            }
        }
        response.setEjercicios(ejerciciosDTO);
        return response;
    }

    private ResultadosResponseDTO mapearResultadosADto(IntentoTest intento) {
        ResultadosResponseDTO response = new ResultadosResponseDTO();
        response.setIdIntento(intento.getId());
        response.setCantidadAciertos(intento.getCantidadAciertos());
        response.setPercentil(intento.getPercentil());
        response.setPuntuacionTotal(intento.getPuntuacionTotal());
        response.setMensaje("Test completado y evaluado con éxito.");
        return response;
    }

    private Candidato buscarCandidatoPorIdentificacion(EntityManager em, String identificacion) {
        try {
            return (Candidato) em.createQuery("select c from Candidato c where c.identificacion = :id")
                    .setParameter("id", identificacion)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private <T> T leerJsonBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        String jsonBody = req.getReader().lines().reduce("", (acc, actual) -> acc + actual);
        return mapper.readValue(jsonBody, clazz);
    }

    private void enviarJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    private void enviarError(HttpServletResponse resp, int statusCode, String mensaje) throws IOException {
        resp.setStatus(statusCode);
        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", mensaje);
        enviarJson(resp, error);
    }

    private void configurarCORS(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}

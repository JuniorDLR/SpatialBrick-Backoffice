package ni.spatialBrick.SpatialBrick.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ni.spatialBrick.SpatialBrick.api.dto.CandidatoRequestDTO;
import ni.spatialBrick.SpatialBrick.api.dto.CandidatoResponseDTO;
import ni.spatialBrick.SpatialBrick.api.dto.CandidatoTestStatusDTO;
import ni.spatialBrick.SpatialBrick.api.dto.EjercicioDTO;
import ni.spatialBrick.SpatialBrick.api.dto.RespuestaDTO;
import ni.spatialBrick.SpatialBrick.api.dto.ResultadosRequestDTO;
import ni.spatialBrick.SpatialBrick.api.dto.ResultadosResponseDTO;
import ni.spatialBrick.SpatialBrick.api.dto.TestResponseDTO;
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
            enviarError(resp, 400, "Error procesando la solicitud: " + e.getMessage());
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
        } else if (path.matches("/candidatos/.+/login")) {
            String identificacion = path.split("/")[2];
            loginCandidato(identificacion, resp);
        } else if (path.matches("/candidatos/.+/tests")) {
            String identificacion = path.split("/")[2];
            obtenerTestsCandidato(identificacion, resp);
        } else {
            enviarError(resp, 404, "Ruta no encontrada: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        
        if (path.equals("/candidatos/registro")) {
            CandidatoRequestDTO request = leerJsonBody(req, CandidatoRequestDTO.class);
            registrarCandidato(request, resp);
        } else if (path.matches("/candidatos/.+/test/.+/iniciar")) {
            String[] partes = path.split("/");
            String identificacion = partes[2];
            String codigoTest = partes[4];
            iniciarTest(identificacion, codigoTest, resp);
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

    private void loginCandidato(String identificacion, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        Candidato candidato = buscarCandidatoPorIdentificacion(em, identificacion);
        if (candidato == null) {
            enviarError(resp, 404, "Candidato no encontrado.");
            return;
        }
        
        CandidatoResponseDTO dto = new CandidatoResponseDTO();
        dto.setIdentificacion(candidato.getIdentificacion());
        dto.setNombreCompleto(candidato.getNombreCompleto());
        
        enviarJson(resp, dto);
    }

    private void registrarCandidato(CandidatoRequestDTO request, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        try {
            Candidato candidato = buscarCandidatoPorIdentificacion(em, request.getIdentificacion());
            if (candidato == null) {
                candidato = new Candidato();
                candidato.setIdentificacion(request.getIdentificacion());
            }

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

            CandidatoResponseDTO dto = new CandidatoResponseDTO();
            dto.setIdentificacion(candidato.getIdentificacion());
            dto.setNombreCompleto(candidato.getNombreCompleto());

            resp.setStatus(201);
            enviarJson(resp, dto);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().setRollbackOnly();
            }
            enviarError(resp, 400, e.getMessage() != null ? e.getMessage() : "Error al registrar el candidato.");
        }
    }

    private void obtenerTestsCandidato(String identificacion, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        Candidato candidato = buscarCandidatoPorIdentificacion(em, identificacion);
        if (candidato == null) {
            enviarError(resp, 404, "Candidato no encontrado.");
            return;
        }

        List<CandidatoTestStatusDTO> tests = new ArrayList<>();
        
        // Verificamos el BFA
        try {
            TestLadrillosCubos testBfa = (TestLadrillosCubos) em.createQuery(
                    "select t from TestLadrillosCubos t where t.codigoTest = 'BFA'")
                    .getSingleResult();
            
            CandidatoTestStatusDTO dto = new CandidatoTestStatusDTO();
            dto.setCodigoTest(testBfa.getCodigoTest());
            dto.setNombreTest("Batería de Funciones Aptitudinales");
            
            if (!testBfa.isActivo()) {
                dto.setEstado("INACTIVO");
            } else {
                // Verificar si ya tiene un intento completado
                Long terminados = (Long) em.createQuery("select count(i) from IntentoTest i where i.candidato.id = :candidatoId and i.test.id = :testId and i.estado = :estado")
                    .setParameter("candidatoId", candidato.getId())
                    .setParameter("testId", testBfa.getId())
                    .setParameter("estado", EstadoIntento.FINALIZADO)
                    .getSingleResult();
                
                if (terminados > 0) {
                    dto.setEstado("COMPLETADO");
                } else {
                    dto.setEstado("DISPONIBLE");
                }
            }
            tests.add(dto);
        } catch (NoResultException e) {
            // El test BFA no está en la base de datos
        }
        
        enviarJson(resp, tests);
    }

    private void iniciarTest(String identificacion, String codigoTest, HttpServletResponse resp) throws IOException {
        EntityManager em = XPersistence.getManager();
        try {
            Candidato candidato = buscarCandidatoPorIdentificacion(em, identificacion);
            if (candidato == null) {
                enviarError(resp, 404, "Candidato no encontrado.");
                return;
            }

            TestLadrillosCubos test;
            try {
                test = (TestLadrillosCubos) em.createQuery(
                        "select t from TestLadrillosCubos t where t.codigoTest = :codigo")
                        .setParameter("codigo", codigoTest)
                        .getSingleResult();
            } catch (NoResultException e) {
                enviarError(resp, 404, "El test con código " + codigoTest + " no existe.");
                return;
            }

            if (!test.isActivo()) {
                enviarError(resp, 403, "Este test está desactivado.");
                return;
            }

            // Verificar si ya lo terminó
            Long terminados = (Long) em.createQuery("select count(i) from IntentoTest i where i.candidato.id = :candidatoId and i.test.id = :testId and i.estado = :estado")
                .setParameter("candidatoId", candidato.getId())
                .setParameter("testId", test.getId())
                .setParameter("estado", EstadoIntento.FINALIZADO)
                .getSingleResult();

            if (terminados > 0) {
                enviarError(resp, 400, "Ya has completado este test.");
                return;
            }

            IntentoTest intento = candidato.iniciarTest(test);
            em.persist(intento);

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("idIntento", intento.getId());
            responseData.put("mensaje", "Intento iniciado.");

            resp.setStatus(201);
            enviarJson(resp, responseData);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().setRollbackOnly();
            }
            enviarError(resp, 400, e.getMessage() != null ? e.getMessage() : "Error interno.");
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
            intento.setMinutosConsumidos(request.getMinutosConsumidos());
            intento.setSegundosConsumidos(request.getSegundosConsumidos());

            // Inyectar las respuestas (en orden estricto y asignando el ejercicio)
            if (request.getRespuestas() != null && intento.getTest() != null && intento.getTest().getEjercicios() != null) {
                List<EjercicioCubos> ejercicios = intento.getTest().getEjercicios();
                int numEjercicios = ejercicios.size();
                List<RespuestaCandidato> ordenadas = new ArrayList<>();
                
                for (EjercicioCubos ej : ejercicios) {
                    RespuestaCandidato r = new RespuestaCandidato();
                    r.setEjercicio(ej);
                    r.setOpcionElegida(ni.spatialBrick.SpatialBrick.modelo.enumeraciones.OpcionRespuesta.OMITIDA);
                    ordenadas.add(r);
                }

                for (RespuestaDTO resDto : request.getRespuestas()) {
                    int num = resDto.getNumeroEjercicio();
                    if (num >= 1 && num <= numEjercicios) {
                        ordenadas.get(num - 1).setOpcionElegida(resDto.getOpcionElegida());
                    }
                }

                for (RespuestaCandidato r : ordenadas) {
                    intento.agregarRespuesta(r);
                }
            }

            intento.setEstado(EstadoIntento.FINALIZADO);
            intento.calcularPuntuacionFinal();

            em.merge(intento);

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
            int contador = 1;
            for (EjercicioCubos ej : test.getEjercicios()) {
                EjercicioDTO dto = new EjercicioDTO();
                dto.setNumeroEjercicio(contador++);
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

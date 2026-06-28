# SpatialBrick - API de Evaluación Psicométrica

**SpatialBrick** es el motor backend (API REST) encargado de gestionar pruebas y evaluaciones psicométricas de razonamiento espacial (Test de Bloques/Cubos). 

El sistema está diseñado para integrarse con clientes Frontend (aplicaciones web o móviles) y gestionar el ciclo de vida completo de la evaluación de un candidato: desde su registro inicial, pasando por la entrega del examen dinámico y la descarga de imágenes, hasta el cálculo automático de puntuaciones, aciertos, y percentiles finales basado en las respuestas elegidas.

## 🛠️ Stack Tecnológico (Backend)

Este proyecto está construido íntegramente sobre una arquitectura robusta, monolítica y modular, utilizando las siguientes tecnologías:

- **Java 17**: Lenguaje principal de desarrollo, utilizando las últimas características del lenguaje para un código limpio y eficiente.
- **OpenXava (v7.7.2)**: Framework principal para la gestión rápida de entidades, persistencia y generación de interfaces de administración Back-Office (CRUDs automatizados).
- **JPA / Hibernate**: ORM utilizado para el mapeo objeto-relacional de las entidades del modelo de negocio, gestionando las transacciones de manera transparente.
- **Servlet API (Java EE)**: Capa HTTP nativa utilizada para exponer los endpoints REST personalizados de alto rendimiento (`RestApiServlet`), logrando integraciones sin fricción.
- **Jackson (v2.18.6)**: Librería estándar y de alta velocidad utilizada para la serialización y deserialización de las estructuras de datos JSON (Requests y DTOs).
- **Maven**: Gestor de dependencias y construcción del proyecto.

## 🚀 Arquitectura de la API REST

El API expone **4 endpoints principales** montados en la ruta base `/rest/`, los cuales manejan toda la lógica transaccional y la validación anti-fraude (Path Traversal, inyecciones, estados inconsistentes de examen).

### 1. Iniciar Test
Registra o actualiza al candidato y crea un intento de test en estado "INICIADO".
- **Método**: `POST`
- **Ruta**: `/rest/candidatos/iniciar-test`
- **Body Esperado** (`application/json`):
  ```json
  {
    "identificacion": "001-251095-1006Q",
    "nombreCompleto": "Axel Junior",
    "nivelEducativo": "UNIVERSITARIO",
    "genero": "MASCULINO",
    "fechaNacimiento": "1995-10-25T00:00:00Z",
    "email": "axel@email.com",
    "telefono": "+505 8888 8888",
    "puestoAplica": "DESARROLLO_VIDEOJUEGOS",
    "profesion": "Ingeniero",
    "codigoTest": "LCA-001"
  }
  ```
- **Respuesta Exitosa** (`201 Created`):
  ```json
  {
    "idIntento": 1,
    "mensaje": "Candidato registrado e intento de test iniciado correctamente."
  }
  ```

### 2. Obtener Estructura del Test
Descarga la metadata del examen, tiempo límite y listado de ejercicios.
- **Método**: `GET`
- **Ruta**: `/rest/test/{codigoTest}` *(Ej. `/rest/test/LCA-001`)*
- **Respuesta Exitosa** (`200 OK`):
  ```json
  {
    "codigoTest": "LCA-001",
    "instrucciones": "Visualiza la figura...",
    "tiempoLimiteSegundos": 3600,
    "ejercicios": [
      {
        "numeroEjercicio": 1,
        "imagenUrl": "/rest/test/imagen/402881e58832a..."
      }
    ]
  }
  ```

### 3. Descargar Imagen del Ejercicio
Endpoint binario de solo lectura. Sirve el archivo visual de un ejercicio psicométrico específico para el Frontend.
- **Método**: `GET`
- **Ruta**: `/rest/test/imagen/{idImagen}`
- **Respuesta Exitosa** (`200 OK`): Archivo de imagen binaria (Stream JPG/PNG). *(No requiere JSON body, se consume vía tag HTML `<img>`).*

### 4. Finalizar y Evaluar Test
Procesa las elecciones del candidato, califica el examen cruzando las opciones correctas y cierra el intento.
- **Método**: `POST`
- **Ruta**: `/rest/intentos/{idIntento}/finalizar`
- **Body Esperado** (`application/json`):
  ```json
  {
    "tiempoConsumido": "_20_MINUTOS",
    "respuestas": [
      {
        "numeroEjercicio": 1,
        "opcionElegida": "B"
      },
      {
        "numeroEjercicio": 2,
        "opcionElegida": "A"
      }
    ]
  }
  ```
- **Respuesta Exitosa** (`200 OK`):
  ```json
  {
    "idIntento": 1,
    "cantidadAciertos": 1,
    "percentil": 50.0,
    "puntuacionTotal": 3.0,
    "mensaje": "Test completado y evaluado con éxito."
  }
  ```

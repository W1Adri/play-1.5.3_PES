package controllers;

import models.*;
import play.mvc.Controller;
import play.mvc.Before; // Importar @Before
import play.libs.Crypto; // Importar Crypto para contraseñas
import play.libs.JSON;
import play.Play;
import play.Logger;
import org.apache.commons.codec.binary.Base64;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Application extends Controller {

    // --- FILTRO DE SEGURIDAD ---
    @Before(unless={
        "index",
        "login",
        "register",
        "logout",
        "apiPing",
        "apiRegister",
        "apiLogin"
})
static void checkAutenticacion() {

    if (session.get("username") == null) {

        // 👉 Si es llamada API → devolver JSON
        if (request != null && request.path != null && request.path.startsWith("/api/")) {

            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("status", "error");
            resp.put("msg", "No logueado");
            renderJSON(resp);
            return;
        }

        // 👉 Si es web → mostrar login normal
        flash.error("Por favor, inicia sesión.");
        index();
        return;
    }
}


    // --- INICIO ---
    public static void index() {
        if (session.get("username") != null) {
            Usuario u = connected();
            if (u == null) {
                session.clear();
                flash.error("Tu sesión ha expirado, inicia sesión nuevamente.");
            } else if (u.rol == Rol.ALUMNO) {
                panelAlumno();
                return;
            } else if (u.rol == Rol.PROFESOR) {
                panelProfesor();
                return;
            }
        }
        render("Application/index.html");
    }

    // --- REGISTRO ---
    public static void register(String username, String password, String email, String fullName, String rol) {
        if (username == null || username.trim().isEmpty()) renderText("El nombre de usuario no puede estar vacío.");
        if (password == null || password.trim().isEmpty()) renderText("La contraseña no puede estar vacía.");
        if (email == null || email.trim().isEmpty()) renderText("El email no puede estar vacío.");
        if (fullName == null || fullName.trim().isEmpty()) renderText("El nombre completo no puede estar vacío.");

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        fullName = fullName.trim();

        Rol rolEnum;
        if ("alumno".equalsIgnoreCase(rol)) {
            rolEnum = Rol.ALUMNO;
        } else if ("profesor".equalsIgnoreCase(rol)) {
            rolEnum = Rol.PROFESOR;
        } else {
            renderText("Rol inválido (debe ser 'alumno' o 'profesor').");
            return;
        }

        if (Usuario.find("byUsername", username).first() != null) renderText("El usuario ya existe.");
        if (Usuario.find("byEmail", email).first() != null) renderText("El email ya está en uso.");

        String passwordHash = Crypto.passwordHash(password);

        new Usuario(username, passwordHash, email, fullName, rolEnum).save();
        renderText("Usuario registrado correctamente como " + rol + ": " + username);
    }


    // --- LOGIN ---
    public static void login(String username, String password) {
        if (username == null || password == null) renderText("Faltan credenciales.");
        username = username.trim().toLowerCase();

        Usuario u = Usuario.find("byUsername", username).first();
        if (u == null) {
            flash.error("Usuario o contraseña incorrectos");
            index();
        }

        String passwordHash = Crypto.passwordHash(password);
        if (!passwordHash.equals(u.passwordHash)) {
            flash.error("Usuario o contraseña incorrectos");
            index();
        }

        session.put("username", u.username);

        if (u.rol == Rol.ALUMNO) panelAlumno();
        else if (u.rol == Rol.PROFESOR) panelProfesor();
        else {
            flash.error("Rol desconocido para este usuario.");
            logout();
        }
    }


    // --- OBTENER USUARIO CONECTADO ---
    public static Usuario connected() {
        String username = session.get("username");
        if (username == null) return null;
        return Usuario.find("byUsername", username).first();
    }

    // --- PANEL ALUMNO ---
    public static void panelAlumno() {
        Usuario u = connected();
        if (u == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (u.rol != Rol.ALUMNO) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        List<Materia> materias = Materia.findAll();
        List<Materia> inscripciones = new ArrayList<Materia>();
        List<Inscripcion> registros = Inscripcion.find("byAlumno", u).fetch();
        Map<Long, Usuario> profesorPorMateria = new HashMap<Long, Usuario>();

        long totalUsuarios = Usuario.count();
        long totalAlumnos = Usuario.count("rol = ?1", Rol.ALUMNO);
        long totalProfesores = Usuario.count("rol = ?1", Rol.PROFESOR);

        for (Inscripcion i : registros) {
            inscripciones.add(i.materia);
            profesorPorMateria.put(i.materia.id, i.profesor);
        }

        List<Usuario> profesores = Usuario.find("byRol", Rol.PROFESOR).fetch();
        renderTemplate("Application/panelAlumno.html", u, materias, inscripciones, profesores, profesorPorMateria,
                totalUsuarios, totalAlumnos, totalProfesores);
    }

    // --- PANEL PROFESOR ---
    public static void panelProfesor() {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }
        long totalUsuarios = Usuario.count();
        long totalAlumnos = Usuario.count("rol = ?1", Rol.ALUMNO);
        long totalProfesores = Usuario.count("rol = ?1", Rol.PROFESOR);
        List<Inscripcion> misAlumnos = Inscripcion.find("byProfesor", profesor).fetch();
        renderTemplate("Application/panelProfesor.html", profesor, misAlumnos,
                totalUsuarios, totalAlumnos, totalProfesores);
    }

    // --- GESTIÓN DE USUARIOS Y MATERIAS (PROFESOR) ---
    public static void gestion() {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        List<Usuario> usuarios = Usuario.findAll();
        List<Materia> materias = Materia.findAll();
        renderTemplate("Application/gestion.html", profesor, usuarios, materias);
    }

    public static void actualizarUsuario(Long id, String username, String email, String fullName, String rol) {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            flash.error("Usuario no encontrado.");
            gestion();
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            flash.error("El nombre de usuario no puede estar vacío.");
            gestion();
            return;
        }
        if (email == null || email.trim().isEmpty()) {
            flash.error("El email no puede estar vacío.");
            gestion();
            return;
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            flash.error("El nombre completo no puede estar vacío.");
            gestion();
            return;
        }

        username = username.trim().toLowerCase();
        email = email.trim().toLowerCase();
        fullName = fullName.trim();

        Usuario existenteUsername = Usuario.find("byUsername", username).first();
        if (existenteUsername != null && !existenteUsername.id.equals(usuario.id)) {
            flash.error("El nombre de usuario ya está en uso.");
            gestion();
            return;
        }

        Usuario existenteEmail = Usuario.find("byEmail", email).first();
        if (existenteEmail != null && !existenteEmail.id.equals(usuario.id)) {
            flash.error("El email ya está en uso.");
            gestion();
            return;
        }

        Rol rolEnum;
        if ("alumno".equalsIgnoreCase(rol)) {
            rolEnum = Rol.ALUMNO;
        } else if ("profesor".equalsIgnoreCase(rol)) {
            rolEnum = Rol.PROFESOR;
        } else {
            flash.error("Rol inválido.");
            gestion();
            return;
        }

        usuario.username = username;
        usuario.email = email;
        usuario.fullName = fullName;
        usuario.rol = rolEnum;
        usuario.save();

        flash.success("Usuario actualizado correctamente.");
        gestion();
    }

    public static void eliminarUsuario(Long id) {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        Usuario usuario = Usuario.findById(id);
        if (usuario == null) {
            flash.error("Usuario no encontrado.");
            gestion();
            return;
        }

        if (usuario.id.equals(profesor.id)) {
            flash.error("No puedes eliminar tu propio usuario.");
            gestion();
            return;
        }

        long inscripcionesAlumno = Inscripcion.count("alumno = ?1", usuario);
        long inscripcionesProfesor = Inscripcion.count("profesor = ?1", usuario);
        long reservasAlumno = Reserva.count("alumno = ?1", usuario);
        long reservasProfesor = Reserva.count("profesor = ?1", usuario);
        long mensajesEnviados = Mensaje.count("emisor = ?1", usuario);
        long mensajesRecibidos = Mensaje.count("receptor = ?1", usuario);

        if (inscripcionesAlumno + inscripcionesProfesor + reservasAlumno + reservasProfesor + mensajesEnviados + mensajesRecibidos > 0) {
            flash.error("No se puede eliminar: el usuario tiene relaciones activas.");
            gestion();
            return;
        }

        usuario.delete();
        flash.success("Usuario eliminado correctamente.");
        gestion();
    }

    public static void actualizarMateria(Long id, String codigo, String nombre, String descripcion) {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        Materia materia = Materia.findById(id);
        if (materia == null) {
            flash.error("Materia no encontrada.");
            gestion();
            return;
        }

        if (codigo == null || codigo.trim().isEmpty()) {
            flash.error("El código no puede estar vacío.");
            gestion();
            return;
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            flash.error("El nombre no puede estar vacío.");
            gestion();
            return;
        }

        codigo = codigo.trim().toUpperCase();
        nombre = nombre.trim();
        descripcion = descripcion != null ? descripcion.trim() : null;

        Materia existente = Materia.find("byCodigo", codigo).first();
        if (existente != null && !existente.id.equals(materia.id)) {
            flash.error("El código ya está en uso.");
            gestion();
            return;
        }

        materia.codigo = codigo;
        materia.nombre = nombre;
        materia.descripcion = descripcion;
        materia.save();

        flash.success("Materia actualizada correctamente.");
        gestion();
    }

    public static void eliminarMateria(Long id) {
        Usuario profesor = connected();
        if (profesor == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (profesor.rol != Rol.PROFESOR) {
            flash.error("Acceso no autorizado.");
            logout();
        }

        Materia materia = Materia.findById(id);
        if (materia == null) {
            flash.error("Materia no encontrada.");
            gestion();
            return;
        }

        long inscripciones = Inscripcion.count("materia = ?1", materia);
        long reservas = Reserva.count("materia = ?1", materia);
        if (inscripciones + reservas > 0) {
            flash.error("No se puede eliminar: la materia tiene relaciones activas.");
            gestion();
            return;
        }

        materia.delete();
        flash.success("Materia eliminada correctamente.");
        gestion();
    }

    // --- RESERVAS DE CLASE ---
    public static void reservas() {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        List<Reserva> reservas;
        List<Inscripcion> opciones;

        if (yo.rol == Rol.ALUMNO) {
            reservas = Reserva.find("alumno = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byAlumno", yo).fetch();
        } else if (yo.rol == Rol.PROFESOR) {
            reservas = Reserva.find("profesor = ?1 ORDER BY fechaReserva ASC", yo).fetch();
            opciones = Inscripcion.find("byProfesor", yo).fetch();
        } else {
            flash.error("Rol no soportado para reservas.");
            index();
            return;
        }

        Date ahora = new Date();
        renderTemplate("Application/reservas.html", yo, reservas, opciones, ahora);
    }

    public static void crearReserva(Long inscripcionId, String fecha, String hora) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        if (inscripcionId == null) {
            flash.error("Selecciona una inscripción válida.");
            reservas();
            return;
        }

        Inscripcion inscripcion = Inscripcion.findById(inscripcionId);
        if (inscripcion == null) {
            flash.error("La inscripción seleccionada no existe.");
            reservas();
            return;
        }

        if (!yo.id.equals(inscripcion.alumno.id) && !yo.id.equals(inscripcion.profesor.id)) {
            flash.error("No tienes permiso para crear reservas con esa inscripción.");
            reservas();
            return;
        }

        if (fecha == null || fecha.trim().isEmpty() || hora == null || hora.trim().isEmpty()) {
            flash.error("Indica la fecha y la hora de la clase.");
            reservas();
            return;
        }

        Date fechaReserva;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            formato.setLenient(false);
            fechaReserva = formato.parse(fecha.trim() + " " + hora.trim());
        } catch (ParseException e) {
            flash.error("Formato de fecha u hora inválido.");
            reservas();
            return;
        }

        Reserva nueva = new Reserva(inscripcion.profesor, inscripcion.alumno, inscripcion.materia, fechaReserva);
        nueva.save();

        flash.success("Reserva creada para " + inscripcion.materia.nombre + " el " + fecha + " a las " + hora + ".");
        reservas();
    }

    public static void video(Long reservaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        Reserva reserva = Reserva.findById(reservaId);
        if (reserva == null) {
            flash.error("La reserva solicitada no existe.");
            reservas();
            return;
        }

        if (!yo.id.equals(reserva.alumno.id) && !yo.id.equals(reserva.profesor.id)) {
            flash.error("No tienes permiso para acceder a esta sala.");
            reservas();
            return;
        }

        String turnUrls = Play.configuration.getProperty("webrtc.turn.urls");
        String turnUsername = Play.configuration.getProperty("webrtc.turn.username");
        String turnCredential = Play.configuration.getProperty("webrtc.turn.credential");
        renderTemplate("Application/video.html", yo, reserva, turnUrls, turnUsername, turnCredential);
    }

    public static void resetearSesionVideo(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        reserva.reiniciarSesion();
        reserva.save();
        Logger.info("[video] Sesion reseteada reservaId=%s alumno=%s profesor=%s", reserva.id, reserva.alumno.username, reserva.profesor.username);

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }
    // --- API PING ---
public static void apiPing() {
    Map<String, Object> resp = new HashMap<String, Object>();
    resp.put("status", "ok");
    resp.put("msg", "Play funcionando");
    renderJSON(resp);
}

// --- API REGISTER ---
public static void apiRegister(String username, String password, String email, String fullName, String rol) {
    Map<String, Object> resp = new HashMap<String, Object>();

    if (username == null) username = getJsonParam("username");
    if (password == null) password = getJsonParam("password");
    if (email == null) email = getJsonParam("email");
    if (fullName == null) fullName = getJsonParam("fullName");
    if (rol == null) rol = getJsonParam("rol");

    if (username == null || password == null || email == null || fullName == null || rol == null) {
        resp.put("status", "error");
        resp.put("msg", "Faltan campos");
        renderJSON(resp);
        return;
    }

    username = username.trim().toLowerCase();
    email = email.trim().toLowerCase();
    fullName = fullName.trim();

    Rol rolEnum;
    if ("alumno".equalsIgnoreCase(rol)) rolEnum = Rol.ALUMNO;
    else if ("profesor".equalsIgnoreCase(rol)) rolEnum = Rol.PROFESOR;
    else {
        resp.put("status", "error");
        resp.put("msg", "Rol inválido");
        renderJSON(resp);
        return;
    }

    if (Usuario.find("byUsername", username).first() != null) {
        resp.put("status", "error");
        resp.put("msg", "El usuario ya existe");
        renderJSON(resp);
        return;
    }

    if (Usuario.find("byEmail", email).first() != null) {
        resp.put("status", "error");
        resp.put("msg", "El email ya está en uso");
        renderJSON(resp);
        return;
    }

    String passwordHash = Crypto.passwordHash(password);
    Usuario u = new Usuario(username, passwordHash, email, fullName, rolEnum).save();

    resp.put("status", "ok");
    resp.put("msg", "Usuario registrado");
    resp.put("userId", u.id);
    renderJSON(resp);
}

// --- API LOGIN ---
public static void apiLogin(String username, String password) {
    Map<String, Object> resp = new HashMap<String, Object>();

    if (username == null) username = getJsonParam("username");
    if (password == null) password = getJsonParam("password");

    if (username == null || password == null) {
        resp.put("status", "error");
        resp.put("msg", "Faltan credenciales");
        renderJSON(resp);
        return;
    }

    username = username.trim().toLowerCase();
    String passwordHash = Crypto.passwordHash(password);

    Usuario u = Usuario.find("byUsernameAndPasswordHash", username, passwordHash).first();

    if (u == null) {
        resp.put("status", "error");
        resp.put("msg", "Usuario o contraseña incorrectos");
        renderJSON(resp);
        return;
    }

    session.put("username", u.username);

    Map<String, Object> user = new HashMap<String, Object>();
    user.put("id", u.id);
    user.put("username", u.username);
    user.put("fullName", u.fullName);
    user.put("rol", u.rol.toString());

    resp.put("status", "ok");
    resp.put("msg", "Login OK");
    resp.put("user", user);
    renderJSON(resp);
}

// --- API ME (ver si hay sesión) ---
public static void apiMe() {
    Map<String, Object> resp = new HashMap<String, Object>();
    Usuario u = connected();

    if (u == null) {
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    Map<String, Object> user = new HashMap<String, Object>();
    user.put("id", u.id);
    user.put("username", u.username);
    user.put("fullName", u.fullName);
    user.put("rol", u.rol.toString());

    resp.put("status", "ok");
    resp.put("user", user);
    renderJSON(resp);
}

// --- API LOGOUT ---
public static void apiLogout() {
    session.clear();
    Map<String, Object> resp = new HashMap<String, Object>();
    resp.put("status", "ok");
    resp.put("msg", "Logout OK");
    renderJSON(resp);
}

// --- API MATERIAS ---
public static void apiMaterias(String soloInscritas) {
    Usuario u = connected();
    if (u == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    boolean solo = "true".equalsIgnoreCase(soloInscritas);
    List<Materia> materias = Materia.findAll();
    Set<Long> inscritas = new HashSet<Long>();

    if (u.rol == Rol.ALUMNO) {
        List<Inscripcion> inscripciones = Inscripcion.find("byAlumno", u).fetch();
        for (Inscripcion inscripcion : inscripciones) {
            inscritas.add(inscripcion.materia.id);
        }
        if (solo) {
            List<Materia> filtradas = new ArrayList<Materia>();
            for (Materia materia : materias) {
                if (inscritas.contains(materia.id)) {
                    filtradas.add(materia);
                }
            }
            materias = filtradas;
        }
    }

    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (Materia materia : materias) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", materia.id);
        item.put("codigo", materia.codigo);
        item.put("nombre", materia.nombre);
        item.put("descripcion", materia.descripcion);
        item.put("inscrita", inscritas.contains(materia.id));
        data.add(item);
    }

    renderJSON(data);
}

// --- API MATERIA DETALLE ---
public static void apiMateria(Long id) {
    Usuario u = connected();
    if (u == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    Materia materia = Materia.findById(id);
    if (materia == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "Materia no encontrada");
        renderJSON(resp);
        return;
    }

    Map<String, Object> resp = new HashMap<String, Object>();
    resp.put("id", materia.id);
    resp.put("codigo", materia.codigo);
    resp.put("nombre", materia.nombre);
    resp.put("descripcion", materia.descripcion);

    List<Usuario> profesores = Usuario.find("byRol", Rol.PROFESOR).fetch();
    List<Map<String, Object>> profesoresDto = new ArrayList<Map<String, Object>>();
    for (Usuario profesor : profesores) {
        profesoresDto.add(toUsuarioDto(profesor));
    }
    resp.put("profesores", profesoresDto);

    renderJSON(resp);
}

// --- API INSCRIPCIONES ---
public static void apiInscripciones() {
    Usuario u = connected();
    if (u == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    List<Inscripcion> inscripciones;
    if (u.rol == Rol.ALUMNO) {
        inscripciones = Inscripcion.find("byAlumno", u).fetch();
    } else if (u.rol == Rol.PROFESOR) {
        inscripciones = Inscripcion.find("byProfesor", u).fetch();
    } else {
        inscripciones = new ArrayList<Inscripcion>();
    }

    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (Inscripcion inscripcion : inscripciones) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", inscripcion.id);
        item.put("materia", toMateriaDto(inscripcion.materia));
        item.put("alumno", toUsuarioDto(inscripcion.alumno));
        item.put("profesor", toUsuarioDto(inscripcion.profesor));
        data.add(item);
    }
    renderJSON(data);
}

// --- API INSCRIBIRSE ---
public static void apiInscribirse(Long materiaId, Long profesorId) {
    Usuario alumno = connected();
    Map<String, Object> resp = new HashMap<String, Object>();

    if (materiaId == null) materiaId = parseLong(getJsonParam("materiaId"));
    if (profesorId == null) profesorId = parseLong(getJsonParam("profesorId"));

    if (alumno == null || alumno.rol != Rol.ALUMNO) {
        resp.put("status", "error");
        resp.put("msg", "Solo alumnos pueden inscribirse");
        renderJSON(resp);
        return;
    }

    Materia materia = Materia.findById(materiaId);
    Usuario profesor = Usuario.findById(profesorId);
    if (materia == null || profesor == null) {
        resp.put("status", "error");
        resp.put("msg", "Materia o profesor inválido");
        renderJSON(resp);
        return;
    }

    Inscripcion existente = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, materia, profesor).first();
    if (existente != null) {
        resp.put("status", "error");
        resp.put("msg", "Ya estás inscrito en esta materia");
        renderJSON(resp);
        return;
    }

    new Inscripcion(alumno, profesor, materia).save();
    resp.put("status", "ok");
    resp.put("msg", "Inscripción creada");
    renderJSON(resp);
}

// --- API RESERVAS ---
public static void apiReservas() {
    Usuario u = connected();
    if (u == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    List<Reserva> reservas;
    if (u.rol == Rol.ALUMNO) {
        reservas = Reserva.find("alumno = ?1 ORDER BY fechaReserva ASC", u).fetch();
    } else if (u.rol == Rol.PROFESOR) {
        reservas = Reserva.find("profesor = ?1 ORDER BY fechaReserva ASC", u).fetch();
    } else {
        reservas = new ArrayList<Reserva>();
    }

    SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (Reserva reserva : reservas) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", reserva.id);
        item.put("fecha", formato.format(reserva.fechaReserva));
        item.put("materia", toMateriaDto(reserva.materia));
        item.put("profesor", toUsuarioDto(reserva.profesor));
        item.put("alumno", toUsuarioDto(reserva.alumno));
        item.put("codigoSala", reserva.codigoSala);
        data.add(item);
    }
    renderJSON(data);
}

// --- API RESERVA DETALLE ---
public static void apiReserva(Long reservaId) {
    Reserva reserva = obtenerReservaAutorizada(reservaId);
    if (reserva == null) {
        return;
    }

    SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    Map<String, Object> item = new HashMap<String, Object>();
    item.put("id", reserva.id);
    item.put("fecha", formato.format(reserva.fechaReserva));
    item.put("materia", toMateriaDto(reserva.materia));
    item.put("profesor", toUsuarioDto(reserva.profesor));
    item.put("alumno", toUsuarioDto(reserva.alumno));
    item.put("codigoSala", reserva.codigoSala);
    renderJSON(item);
}

// --- API CREAR RESERVA ---
public static void apiCrearReserva(Long inscripcionId, String fecha, String hora) {
    Usuario yo = connected();
    Map<String, Object> resp = new HashMap<String, Object>();
    if (inscripcionId == null) inscripcionId = parseLong(getJsonParam("inscripcionId"));
    if (fecha == null) fecha = getJsonParam("fecha");
    if (hora == null) hora = getJsonParam("hora");
    if (yo == null) {
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    if (inscripcionId == null) {
        resp.put("status", "error");
        resp.put("msg", "Selecciona una inscripción válida");
        renderJSON(resp);
        return;
    }

    Inscripcion inscripcion = Inscripcion.findById(inscripcionId);
    if (inscripcion == null) {
        resp.put("status", "error");
        resp.put("msg", "Inscripción no encontrada");
        renderJSON(resp);
        return;
    }

    if (!yo.id.equals(inscripcion.alumno.id) && !yo.id.equals(inscripcion.profesor.id)) {
        resp.put("status", "error");
        resp.put("msg", "No autorizado");
        renderJSON(resp);
        return;
    }

    if (fecha == null || fecha.trim().isEmpty() || hora == null || hora.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "Indica fecha y hora");
        renderJSON(resp);
        return;
    }

    Date fechaReserva;
    try {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        formato.setLenient(false);
        fechaReserva = formato.parse(fecha.trim() + " " + hora.trim());
    } catch (ParseException e) {
        resp.put("status", "error");
        resp.put("msg", "Formato de fecha u hora inválido");
        renderJSON(resp);
        return;
    }

    Reserva nueva = new Reserva(inscripcion.profesor, inscripcion.alumno, inscripcion.materia, fechaReserva);
    nueva.save();
    resp.put("status", "ok");
    resp.put("msg", "Reserva creada");
    resp.put("reservaId", nueva.id);
    renderJSON(resp);
}

// --- API CONSULTAS ---
public static void apiConsultas(String tipo, Long materiaId) {
    Usuario yo = connected();
    if (yo == null) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "No logueado");
        renderJSON(resp);
        return;
    }

    if (tipo == null) {
        tipo = getJsonParam("tipo");
    }
    if (materiaId == null) {
        materiaId = parseLong(getJsonParam("materiaId"));
    }

    List<Materia> materias = Materia.findAll();
    Materia materiaSeleccionada = null;
    Long resultado = null;
    String descripcion = null;
    String error = null;
    List<Usuario> detalleUsuarios = new ArrayList<Usuario>();
    List<Map<String, Object>> ranking = new ArrayList<Map<String, Object>>();

    if (tipo != null && !tipo.trim().isEmpty()) {
        tipo = tipo.trim();
        if ("totalUsuarios".equals(tipo)) {
            resultado = Usuario.count();
            descripcion = "Usuarios registrados";
        } else if ("totalAlumnos".equals(tipo)) {
            resultado = Usuario.count("rol = ?1", Rol.ALUMNO);
            descripcion = "Alumnos registrados";
        } else if ("totalProfesores".equals(tipo)) {
            resultado = Usuario.count("rol = ?1", Rol.PROFESOR);
            descripcion = "Profesores registrados";
        } else if ("profesoresMateria".equals(tipo) || "alumnosMateria".equals(tipo)) {
            if (materiaId == null) {
                error = "Selecciona una materia para realizar la consulta.";
            } else {
                materiaSeleccionada = Materia.findById(materiaId);
                if (materiaSeleccionada == null) {
                    error = "La materia indicada no existe.";
                } else {
                    List<Inscripcion> inscripciones = Inscripcion.find("byMateria", materiaSeleccionada).fetch();
                    LinkedHashMap<Long, Usuario> usuariosUnicos = new LinkedHashMap<Long, Usuario>();
                    for (Inscripcion inscripcion : inscripciones) {
                        if ("profesoresMateria".equals(tipo) && inscripcion.profesor != null) {
                            usuariosUnicos.put(inscripcion.profesor.id, inscripcion.profesor);
                        }
                        if ("alumnosMateria".equals(tipo) && inscripcion.alumno != null) {
                            usuariosUnicos.put(inscripcion.alumno.id, inscripcion.alumno);
                        }
                    }
                    detalleUsuarios = new ArrayList<Usuario>(usuariosUnicos.values());
                    resultado = Long.valueOf(detalleUsuarios.size());
                    if ("profesoresMateria".equals(tipo)) {
                        descripcion = "Profesores registrados en " + materiaSeleccionada.nombre;
                    } else {
                        descripcion = "Alumnos inscritos en " + materiaSeleccionada.nombre;
                    }
                }
            }
        } else if ("reservasPorMateria".equals(tipo)) {
            List<Object[]> filas = Reserva.find(
                    "select r.materia.nombre, count(r) from Reserva r group by r.materia.nombre order by count(r) desc"
            ).fetch();
            for (Object[] fila : filas) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("label", fila[0]);
                item.put("count", fila[1]);
                ranking.add(item);
            }
            descripcion = "Reservas totales por materia";
        } else if ("topAlumnos".equals(tipo)) {
            List<Object[]> filas = Reserva.find(
                    "select r.alumno.fullName, r.alumno.username, count(r) " +
                            "from Reserva r group by r.alumno.id, r.alumno.fullName, r.alumno.username order by count(r) desc"
            ).fetch();
            for (Object[] fila : filas) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("label", fila[0] + " · " + fila[1]);
                item.put("count", fila[2]);
                ranking.add(item);
            }
            descripcion = "Alumnos con más reservas";
        } else if ("topProfesores".equals(tipo)) {
            List<Object[]> filas = Inscripcion.find(
                    "select i.profesor.fullName, i.profesor.username, count(i) " +
                            "from Inscripcion i group by i.profesor.id, i.profesor.fullName, i.profesor.username order by count(i) desc"
            ).fetch();
            for (Object[] fila : filas) {
                Map<String, Object> item = new HashMap<String, Object>();
                item.put("label", fila[0] + " · " + fila[1]);
                item.put("count", fila[2]);
                ranking.add(item);
            }
            descripcion = "Profesores con más alumnos";
        } else {
            error = "Consulta no reconocida.";
        }
    }

    Map<String, Object> resp = new HashMap<String, Object>();
    resp.put("status", error == null ? "ok" : "error");
    resp.put("error", error);
    resp.put("resultado", resultado);
    resp.put("descripcion", descripcion);
    resp.put("materiaSeleccionada", materiaSeleccionada != null ? toMateriaDto(materiaSeleccionada) : null);

    List<Map<String, Object>> detalleDto = new ArrayList<Map<String, Object>>();
    for (Usuario usuario : detalleUsuarios) {
        detalleDto.add(toUsuarioDto(usuario));
    }
    resp.put("detalleUsuarios", detalleDto);
    resp.put("ranking", ranking);

    List<Map<String, Object>> materiasDto = new ArrayList<Map<String, Object>>();
    for (Materia materia : materias) {
        materiasDto.add(toMateriaDto(materia));
    }
    resp.put("materias", materiasDto);

    renderJSON(resp);
}

// --- API GESTION (PROFESOR) ---
public static void apiGestionUsuarios() {
    Usuario profesor = connected();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    List<Usuario> usuarios = Usuario.findAll();
    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (Usuario usuario : usuarios) {
        Map<String, Object> item = toUsuarioDto(usuario);
        item.put("email", usuario.email);
        data.add(item);
    }
    renderJSON(data);
}

public static void apiGestionMaterias() {
    Usuario profesor = connected();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    List<Materia> materias = Materia.findAll();
    List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
    for (Materia materia : materias) {
        data.add(toMateriaDto(materia));
    }
    renderJSON(data);
}

public static void apiActualizarUsuario(Long id, String username, String email, String fullName, String rol) {
    Usuario profesor = connected();
    Map<String, Object> resp = new HashMap<String, Object>();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    if (id == null) id = parseLong(getJsonParam("id"));
    if (username == null) username = getJsonParam("username");
    if (email == null) email = getJsonParam("email");
    if (fullName == null) fullName = getJsonParam("fullName");
    if (rol == null) rol = getJsonParam("rol");

    Usuario usuario = Usuario.findById(id);
    if (usuario == null) {
        resp.put("status", "error");
        resp.put("msg", "Usuario no encontrado");
        renderJSON(resp);
        return;
    }

    if (username == null || username.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "El nombre de usuario no puede estar vacío");
        renderJSON(resp);
        return;
    }
    if (email == null || email.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "El email no puede estar vacío");
        renderJSON(resp);
        return;
    }
    if (fullName == null || fullName.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "El nombre completo no puede estar vacío");
        renderJSON(resp);
        return;
    }

    Rol nuevoRol;
    if ("ALUMNO".equalsIgnoreCase(rol)) nuevoRol = Rol.ALUMNO;
    else if ("PROFESOR".equalsIgnoreCase(rol)) nuevoRol = Rol.PROFESOR;
    else {
        resp.put("status", "error");
        resp.put("msg", "Rol inválido");
        renderJSON(resp);
        return;
    }

    Usuario existente = Usuario.find("byUsername", username.trim().toLowerCase()).first();
    if (existente != null && !existente.id.equals(usuario.id)) {
        resp.put("status", "error");
        resp.put("msg", "El usuario ya existe");
        renderJSON(resp);
        return;
    }

    Usuario existenteEmail = Usuario.find("byEmail", email.trim().toLowerCase()).first();
    if (existenteEmail != null && !existenteEmail.id.equals(usuario.id)) {
        resp.put("status", "error");
        resp.put("msg", "El email ya está en uso");
        renderJSON(resp);
        return;
    }

    usuario.username = username.trim().toLowerCase();
    usuario.email = email.trim().toLowerCase();
    usuario.fullName = fullName.trim();
    usuario.rol = nuevoRol;
    usuario.save();

    resp.put("status", "ok");
    renderJSON(resp);
}

public static void apiEliminarUsuario(Long id) {
    Usuario profesor = connected();
    Map<String, Object> resp = new HashMap<String, Object>();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    if (id == null) id = parseLong(getJsonParam("id"));
    Usuario usuario = Usuario.findById(id);
    if (usuario == null) {
        resp.put("status", "error");
        resp.put("msg", "Usuario no encontrado");
        renderJSON(resp);
        return;
    }

    long inscripcionesAlumno = Inscripcion.count("alumno = ?1", usuario);
    long inscripcionesProfesor = Inscripcion.count("profesor = ?1", usuario);
    long reservasAlumno = Reserva.count("alumno = ?1", usuario);
    long reservasProfesor = Reserva.count("profesor = ?1", usuario);
    long mensajesEnviados = Mensaje.count("emisor = ?1", usuario);
    long mensajesRecibidos = Mensaje.count("receptor = ?1", usuario);

    if (inscripcionesAlumno + inscripcionesProfesor + reservasAlumno + reservasProfesor + mensajesEnviados + mensajesRecibidos > 0) {
        resp.put("status", "error");
        resp.put("msg", "No se puede eliminar: el usuario tiene relaciones activas.");
        renderJSON(resp);
        return;
    }

    usuario.delete();
    resp.put("status", "ok");
    renderJSON(resp);
}

public static void apiActualizarMateria(Long id, String codigo, String nombre, String descripcion) {
    Usuario profesor = connected();
    Map<String, Object> resp = new HashMap<String, Object>();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    if (id == null) id = parseLong(getJsonParam("id"));
    if (codigo == null) codigo = getJsonParam("codigo");
    if (nombre == null) nombre = getJsonParam("nombre");
    if (descripcion == null) descripcion = getJsonParam("descripcion");

    Materia materia = Materia.findById(id);
    if (materia == null) {
        resp.put("status", "error");
        resp.put("msg", "Materia no encontrada");
        renderJSON(resp);
        return;
    }

    if (codigo == null || codigo.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "El código no puede estar vacío");
        renderJSON(resp);
        return;
    }
    if (nombre == null || nombre.trim().isEmpty()) {
        resp.put("status", "error");
        resp.put("msg", "El nombre no puede estar vacío");
        renderJSON(resp);
        return;
    }

    Materia existente = Materia.find("byCodigo", codigo.trim()).first();
    if (existente != null && !existente.id.equals(materia.id)) {
        resp.put("status", "error");
        resp.put("msg", "El código ya está en uso");
        renderJSON(resp);
        return;
    }

    materia.codigo = codigo.trim();
    materia.nombre = nombre.trim();
    materia.descripcion = descripcion != null ? descripcion.trim() : null;
    materia.save();

    resp.put("status", "ok");
    renderJSON(resp);
}

public static void apiEliminarMateria(Long id) {
    Usuario profesor = connected();
    Map<String, Object> resp = new HashMap<String, Object>();
    if (profesor == null || profesor.rol != Rol.PROFESOR) {
        resp.put("status", "error");
        resp.put("msg", "Acceso no autorizado");
        renderJSON(resp);
        return;
    }

    if (id == null) id = parseLong(getJsonParam("id"));
    Materia materia = Materia.findById(id);
    if (materia == null) {
        resp.put("status", "error");
        resp.put("msg", "Materia no encontrada");
        renderJSON(resp);
        return;
    }

    long inscripciones = Inscripcion.count("materia = ?1", materia);
    long reservas = Reserva.count("materia = ?1", materia);
    if (inscripciones + reservas > 0) {
        resp.put("status", "error");
        resp.put("msg", "No se puede eliminar: la materia tiene relaciones activas.");
        renderJSON(resp);
        return;
    }

    materia.delete();
    resp.put("status", "ok");
    renderJSON(resp);
}



    public static void publicarOffer(Long reservaId, String sdp, String sdp64) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Logger.info("[video] publicarOffer reservaId=%s sdp=%s sdp64=%s", reserva.id, sdp != null ? sdp.length() : null, sdp64 != null ? sdp64.length() : null);
        String[] rawParams = extractSdpParams(sdp, sdp64);
        String decoded = decodeSdp(rawParams[0], rawParams[1]);
        if (decoded == null || decoded.trim().isEmpty()) {
            Logger.warn("[video] publicarOffer SDP vacio reservaId=%s", reserva.id);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "SDP vacío");
            renderJSON(error);
            return;
        }

        reserva.offerSdp = decoded.trim();
        reserva.offerActualizada = new Date();
        reserva.answerSdp = null;
        reserva.answerActualizada = null;
        reserva.save();
        Logger.info("[video] Offer guardada reservaId=%s size=%s", reserva.id, reserva.offerSdp.length());

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }

    public static void publicarAnswer(Long reservaId, String sdp, String sdp64) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Logger.info("[video] publicarAnswer reservaId=%s sdp=%s sdp64=%s", reserva.id, sdp != null ? sdp.length() : null, sdp64 != null ? sdp64.length() : null);
        String[] rawParams = extractSdpParams(sdp, sdp64);
        String decoded = decodeSdp(rawParams[0], rawParams[1]);
        if (decoded == null || decoded.trim().isEmpty()) {
            Logger.warn("[video] publicarAnswer SDP vacio reservaId=%s", reserva.id);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "SDP vacío");
            renderJSON(error);
            return;
        }

        reserva.answerSdp = decoded.trim();
        reserva.answerActualizada = new Date();
        reserva.save();
        Logger.info("[video] Answer guardada reservaId=%s size=%s", reserva.id, reserva.answerSdp.length());

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("status", "ok");
        renderJSON(resp);
    }

    public static void obtenerOffer(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Logger.debug("[video] obtenerOffer reservaId=%s sdp=%s", reserva.id, reserva.offerSdp != null ? reserva.offerSdp.length() : null);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("sdp", reserva.offerSdp);
        resp.put("type", reserva.offerSdp != null ? "offer" : null);
        resp.put("timestamp", reserva.offerActualizada != null ? reserva.offerActualizada.getTime() : null);
        renderJSON(resp);
    }

    public static void obtenerAnswer(Long reservaId) {
        Reserva reserva = obtenerReservaAutorizada(reservaId);
        if (reserva == null) return;

        Logger.debug("[video] obtenerAnswer reservaId=%s sdp=%s", reserva.id, reserva.answerSdp != null ? reserva.answerSdp.length() : null);
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("sdp", reserva.answerSdp);
        resp.put("type", reserva.answerSdp != null ? "answer" : null);
        resp.put("timestamp", reserva.answerActualizada != null ? reserva.answerActualizada.getTime() : null);
        renderJSON(resp);
    }

    private static Reserva obtenerReservaAutorizada(Long reservaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return null;
        }

        Reserva reserva = Reserva.findById(reservaId);
        if (reserva == null) {
            Logger.warn("[video] Reserva no encontrada reservaId=%s usuario=%s", reservaId, yo.username);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Reserva no encontrada");
            renderJSON(error);
            return null;
        }

        if (!yo.id.equals(reserva.alumno.id) && !yo.id.equals(reserva.profesor.id)) {
            Logger.warn("[video] No autorizado reservaId=%s usuario=%s", reservaId, yo.username);
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "No autorizado");
            renderJSON(error);
            return null;
        }

        Logger.debug("[video] Acceso autorizado reservaId=%s usuario=%s rol=%s", reserva.id, yo.username, yo.rol);
        return reserva;
    }

    private static String decodeSdp(String sdp, String sdp64) {
        if (sdp64 != null && !sdp64.trim().isEmpty()) {
            try {
                String normalized = normalizeBase64Url(sdp64.trim());
                String decoded = new String(Base64.decodeBase64(normalized), "UTF-8");
                decoded = normalizeSdpString(decoded);
                Logger.debug("[video] decodeSdp usando sdp64 size=%s decoded=%s", sdp64.length(), decoded.length());
                return decoded;
            } catch (Exception e) {
                Logger.error(e, "[video] Error decodificando sdp64");
                return null;
            }
        }
        if (sdp != null) {
            return normalizeSdpString(sdp);
        }
        Logger.debug("[video] decodeSdp usando sdp plano size=%s", sdp != null ? sdp.length() : null);
        return sdp;
    }

    private static String normalizeSdpString(String sdp) {
        if (sdp == null) {
            return null;
        }
        String normalized = sdp;
        normalized = normalized.replace("\\\\r\\\\n", "\r\n");
        normalized = normalized.replace("\\\\n", "\n");
        normalized = normalized.replace("\\\\r", "\r");
        normalized = normalized.replace("\\r\\n", "\r\n");
        normalized = normalized.replace("\\n", "\n");
        normalized = normalized.replace("\\r", "\r");
        return normalized;
    }

    private static String normalizeBase64Url(String value) {
        String normalized = value.replace('-', '+').replace('_', '/');
        int padding = normalized.length() % 4;
        if (padding > 0) {
            normalized = normalized + "====".substring(padding);
        }
        return normalized;
    }

    private static String[] extractSdpParams(String sdp, String sdp64) {
        String resolvedSdp = firstNonBlank(sdp, params.get("sdp"));
        String resolvedSdp64 = firstNonBlank(sdp64, params.get("sdp64"));

        if ((resolvedSdp == null || resolvedSdp64 == null) && request != null && request.body != null) {
            String body = readRequestBody();
            if (body != null && !body.trim().isEmpty()) {
                if (resolvedSdp == null) {
                    resolvedSdp = extractFormValue(body, "sdp");
                }
                if (resolvedSdp64 == null) {
                    resolvedSdp64 = extractFormValue(body, "sdp64");
                }
            }
        }

        return new String[] { resolvedSdp, resolvedSdp64 };
    }

    private static String readRequestBody() {
        try {
            return play.libs.IO.readContentAsString(request.body);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String getJsonParam(String key) {
        String body = readRequestBody();
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement element = JSON.parse(body);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement value = obj.get(key);
                if (value != null && !value.isJsonNull()) {
                    return value.getAsString();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String extractFormValue(String body, String key) {
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String name = pair.substring(0, idx);
            if (!key.equals(name)) continue;
            String value = pair.substring(idx + 1);
            return decodeUrlComponent(value);
        }
        return null;
    }

    private static String decodeUrlComponent(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (Exception ex) {
            return value;
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        if (second != null && !second.trim().isEmpty()) {
            return second;
        }
        return null;
    }

    // --- CONSULTAS PERSONALIZADAS ---
    public static void consultas(String tipo, Long materiaId) {
        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        List<Materia> materias = Materia.findAll();
        Materia materiaSeleccionada = null;
        Long resultado = null;
        String descripcion = null;
        String error = null;
        List<Usuario> detalleUsuarios = new ArrayList<Usuario>();
        List<Map<String, Object>> ranking = new ArrayList<Map<String, Object>>();

        if (tipo != null && !tipo.trim().isEmpty()) {
            tipo = tipo.trim();
            if ("totalUsuarios".equals(tipo)) {
                resultado = Usuario.count();
                descripcion = "Usuarios registrados";
            } else if ("totalAlumnos".equals(tipo)) {
                resultado = Usuario.count("rol = ?1", Rol.ALUMNO);
                descripcion = "Alumnos registrados";
            } else if ("totalProfesores".equals(tipo)) {
                resultado = Usuario.count("rol = ?1", Rol.PROFESOR);
                descripcion = "Profesores registrados";
            } else if ("profesoresMateria".equals(tipo) || "alumnosMateria".equals(tipo)) {
                if (materiaId == null) {
                    error = "Selecciona una materia para realizar la consulta.";
                } else {
                    materiaSeleccionada = Materia.findById(materiaId);
                    if (materiaSeleccionada == null) {
                        error = "La materia indicada no existe.";
                    } else {
                        List<Inscripcion> inscripciones = Inscripcion.find("byMateria", materiaSeleccionada).fetch();
                        LinkedHashMap<Long, Usuario> usuariosUnicos = new LinkedHashMap<Long, Usuario>();
                        for (Inscripcion inscripcion : inscripciones) {
                            if ("profesoresMateria".equals(tipo) && inscripcion.profesor != null) {
                                usuariosUnicos.put(inscripcion.profesor.id, inscripcion.profesor);
                            }
                            if ("alumnosMateria".equals(tipo) && inscripcion.alumno != null) {
                                usuariosUnicos.put(inscripcion.alumno.id, inscripcion.alumno);
                            }
                        }
                        detalleUsuarios = new ArrayList<Usuario>(usuariosUnicos.values());
                        resultado = Long.valueOf(detalleUsuarios.size());
                        if ("profesoresMateria".equals(tipo)) {
                            descripcion = "Profesores registrados en " + materiaSeleccionada.nombre;
                        } else {
                            descripcion = "Alumnos inscritos en " + materiaSeleccionada.nombre;
                        }
                    }
                }
            } else if ("reservasPorMateria".equals(tipo)) {
                List<Object[]> filas = Reserva.find(
                        "select r.materia.nombre, count(r) from Reserva r group by r.materia.nombre order by count(r) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0]);
                    item.put("count", fila[1]);
                    ranking.add(item);
                }
                descripcion = "Reservas totales por materia";
            } else if ("topAlumnos".equals(tipo)) {
                List<Object[]> filas = Reserva.find(
                        "select r.alumno.fullName, r.alumno.username, count(r) " +
                                "from Reserva r group by r.alumno.id, r.alumno.fullName, r.alumno.username order by count(r) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0] + " · " + fila[1]);
                    item.put("count", fila[2]);
                    ranking.add(item);
                }
                descripcion = "Alumnos con más reservas";
            } else if ("topProfesores".equals(tipo)) {
                List<Object[]> filas = Inscripcion.find(
                        "select i.profesor.fullName, i.profesor.username, count(i) " +
                                "from Inscripcion i group by i.profesor.id, i.profesor.fullName, i.profesor.username order by count(i) desc"
                ).fetch();
                for (Object[] fila : filas) {
                    Map<String, Object> item = new HashMap<String, Object>();
                    item.put("label", fila[0] + " · " + fila[1]);
                    item.put("count", fila[2]);
                    ranking.add(item);
                }
                descripcion = "Profesores con más alumnos";
            } else {
                error = "Consulta no reconocida.";
            }
        }

        renderTemplate("Application/consultas.html", yo, materias, tipo, materiaSeleccionada,
                resultado, descripcion, detalleUsuarios, ranking, error);
    }

    // --- DETALLE DE MATERIA ---
    public static void detalle(Long id) {
        Materia m = Materia.findById(id);
        Usuario u = connected();

        if (u == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }

        if (m == null) {
            flash.error("Materia no encontrada");
            if (u.rol == Rol.ALUMNO) panelAlumno();
            else panelProfesor();
        }

        List<Usuario> profesores = Usuario.find("byRol", Rol.PROFESOR).fetch();
        renderTemplate("Application/detalle.html", m, u, profesores);
    }

    // --- INSCRIPCIÓN ---
    public static void inscribirse(Long id, Long profesorId) {
        Usuario alumno = connected();
        if (alumno.rol != Rol.ALUMNO) {
            flash.error("Solo los alumnos pueden inscribirse.");
            logout();
        }

        Materia m = Materia.findById(id);
        Usuario profesor = Usuario.findById(profesorId);

        if (m != null && profesor != null) {
            Inscripcion existente = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, m, profesor).first();
            if (existente == null) {
                new Inscripcion(alumno, profesor, m).save();
                flash.success("Te has inscrito correctamente en " + m.nombre);
                panelAlumno();
            } else {
                flash.error("Ya estás inscrito en esta materia.");
                panelAlumno();
            }
        } else {
            flash.error("Error al inscribirse (materia o profesor no válidos).");
            panelAlumno();
        }
    }

    // --- VISTA DE CHAT ---
    public static void chat(Long alumnoId, Long profesorId) {
        Usuario alumno = Usuario.findById(alumnoId);
        Usuario profesor = Usuario.findById(profesorId);
        if (alumno == null || profesor == null) renderText("Error: usuario no encontrado");

        Usuario yo = connected();
        if (yo == null) {
            session.clear();
            flash.error("Debes iniciar sesión nuevamente.");
            index();
            return;
        }
        if (!yo.id.equals(alumno.id) && !yo.id.equals(profesor.id)) {
            flash.error("No tienes permiso para ver este chat.");
            index();
        }

        List<Mensaje> mensajes = Mensaje.find(
                "(emisor = ?1 AND receptor = ?2) OR (emisor = ?3 AND receptor = ?4) ORDER BY fecha ASC",
                alumno, profesor, profesor, alumno
        ).fetch();

        List<Mensaje> mensajesValidos = new ArrayList<Mensaje>();
        for (Mensaje mensaje : mensajes) {
            if (mensaje != null && mensaje.emisor != null && mensaje.receptor != null) {
                mensajesValidos.add(mensaje);
            }
        }

        mensajes = mensajesValidos;
        render(alumno, profesor, mensajes, yo);
    }


    // --- ENVIAR MENSAJE (API) ---
    public static void enviarMensaje(Long receptorId, String contenido) {
        Usuario emisor = connected();
        if (receptorId == null) receptorId = parseLong(getJsonParam("receptorId"));
        if (contenido == null) contenido = getJsonParam("contenido");
        Usuario receptor = Usuario.findById(receptorId);

        if (emisor == null || receptor == null) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Usuario no encontrado");
            renderJSON(error);
            return;
        }

        if (!((emisor.rol == Rol.ALUMNO && receptor.rol == Rol.PROFESOR) ||
                (emisor.rol == Rol.PROFESOR && receptor.rol == Rol.ALUMNO))) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Chat no permitido");
            renderJSON(error);
            return;
        }

        if (contenido == null || contenido.trim().isEmpty()) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "Contenido vacío");
            renderJSON(error);
            return;
        }

        Mensaje nuevo = new Mensaje(emisor, receptor, contenido.trim());
        nuevo.save();

        renderJSON(toMensajeDto(nuevo));
    }

    // --- OBTENER MENSAJES (API) ---
    public static void obtenerMensajes(Long alumnoId, Long profesorId, Long lastId) {
        Usuario alumno = Usuario.findById(alumnoId);
        Usuario profesor = Usuario.findById(profesorId);

        if (alumno == null || profesor == null) {
            renderJSON(Collections.emptyList());
            return;
        }

        Usuario yo = connected();
        if (!yo.id.equals(alumno.id) && !yo.id.equals(profesor.id)) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "No autorizado");
            renderJSON(error);
            return;
        }

        String baseQuery = "((emisor = ?1 AND receptor = ?2) OR (emisor = ?3 AND receptor = ?4))";
        List<Mensaje> mensajes;
        if (lastId != null) {
            mensajes = Mensaje.find(baseQuery + " AND id > ?5 ORDER BY fecha ASC",
                    alumno, profesor, profesor, alumno, lastId
            ).fetch();
        } else {
            mensajes = Mensaje.find(baseQuery + " ORDER BY fecha ASC",
                    alumno, profesor, profesor, alumno
            ).fetch();
        }

        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        for (Mensaje mensaje : mensajes) {
            if (mensaje != null && mensaje.emisor != null && mensaje.receptor != null) {
                data.add(toMensajeDto(mensaje));
            }
        }

        renderJSON(data);
    }

    // --- LOGOUT ---
    public static void logout() {
        session.clear();
        flash.success("Has cerrado sesión correctamente.");
        index();
    }

    private static Map<String, Object> toMensajeDto(Mensaje mensaje) {
        Map<String, Object> emisorDto = new HashMap<String, Object>();
        emisorDto.put("id", mensaje.emisor.id);
        emisorDto.put("username", mensaje.emisor.username);

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("id", mensaje.id);
        resp.put("emisor", emisorDto);
        resp.put("contenido", mensaje.contenido);
        resp.put("fecha", mensaje.fecha);

        return resp;
    }

    private static Map<String, Object> toUsuarioDto(Usuario usuario) {
        Map<String, Object> dto = new HashMap<String, Object>();
        if (usuario == null) {
            return dto;
        }
        dto.put("id", usuario.id);
        dto.put("username", usuario.username);
        dto.put("fullName", usuario.fullName);
        dto.put("rol", usuario.rol.toString());
        return dto;
    }

    private static Map<String, Object> toMateriaDto(Materia materia) {
        Map<String, Object> dto = new HashMap<String, Object>();
        if (materia == null) {
            return dto;
        }
        dto.put("id", materia.id);
        dto.put("codigo", materia.codigo);
        dto.put("nombre", materia.nombre);
        dto.put("descripcion", materia.descripcion);
        return dto;
    }
}

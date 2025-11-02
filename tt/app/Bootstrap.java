import play.jobs.Job;
import play.jobs.OnApplicationStart;
import models.*; // Importa TODOS los modelos (Usuario, Rol, Materia, Inscripcion, Mensaje)
import play.libs.Crypto; // Importar Crypto para hashear

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {

        // --- 1. Crear Materias ---
        if (Materia.count() == 0) { // 0L no es necesario, '0' funciona
            new Materia("MAT101", "Matemáticas", "Álgebra y geometría.").save();
            new Materia("PROG1", "Programación I", "Introducción a Java.").save();
            new Materia("ENG202", "Inglés Técnico", "Vocabulario profesional.").save();
        }

        // --- 2. Crear Usuarios ---
        if (Usuario.count() == 0) { // <-- Cambiado a Usuario

            // Hasheamos la contraseña "1234" UNA SOLA VEZ
            String passHash = Crypto.passwordHash("1234");

            // Usamos 'new Usuario', el 'passHash' y el 'Enum Rol'
            new Usuario("laura", passHash, "laura@uni.com", "Laura García", Rol.PROFESOR).save();
            new Usuario("carlos", passHash, "carlos@uni.com", "Carlos Ruiz", Rol.PROFESOR).save();
            new Usuario("marta", passHash, "marta@uni.com", "Marta Fernández", Rol.PROFESOR).save();
            new Usuario("roger", passHash, "roger@uni.com", "Roger Vidal", Rol.ALUMNO).save();
        }

        // --- 3. Crear Inscripción de prueba ---

        // Buscamos los usuarios con el tipo 'Usuario' y la sintaxis limpia
        Usuario alumno = Usuario.find("byUsername", "roger").first();
        Usuario profL = Usuario.find("byUsername", "laura").first();
        Materia mat101 = Materia.find("byCodigo", "MAT101").first();

        if (alumno != null && profL != null && mat101 != null) {
            // Comprobamos si la inscripción ya existe
            Inscripcion ya = Inscripcion.find("byAlumnoAndMateriaAndProfesor", alumno, mat101, profL).first();
            if (ya == null) {
                new Inscripcion(alumno, profL, mat101).save();
            }
        }

        // --- 4. Crear Mensajes de prueba ---
        if (Mensaje.count() == 0) {
            Usuario profC = Usuario.find("byUsername", "carlos").first();
            if (alumno != null && profC != null) {
                new Mensaje(alumno, profC, "Hola profe, ¿podemos ver derivadas mañana?").save();
                new Mensaje(profC, alumno, "¡Claro! A las 17:00 te va bien.").save();
            }
        }
    }
}
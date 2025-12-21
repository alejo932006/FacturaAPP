import java.util.Set;

public class UserSession {

    // Roles que existirán en el sistema
    public enum Rol {
        ADMINISTRADOR,
        CAJERO
    }

    private static String loggedInUsername;
    private static Rol loggedInUserRole;

    public static void login(String username, Rol role) {
        loggedInUsername = username;
        loggedInUserRole = role;
    }

    public static void logout() {
        loggedInUsername = null;
        loggedInUserRole = null;
    }

    public static String getLoggedInUsername() {
        return loggedInUsername;
    }
    
    public static Rol getLoggedInUserRole() {
        return loggedInUserRole;
    }

    /**
     * El nuevo método central para verificar permisos.
     * @param permiso El permiso requerido para realizar una acción.
     * @return true si el rol del usuario actual tiene ese permiso.
     */
    public static boolean tienePermiso(String permiso) {
        if (loggedInUserRole == null) {
            return false; // Si no hay nadie logueado, no hay permisos.
        }

        switch (loggedInUserRole) {
            case ADMINISTRADOR:
                return true; // El admin tiene acceso a todo.

            case CAJERO:
                // El cajero solo tiene acceso a un conjunto específico de permisos.
                Set<String> permisosCajero = Set.of(
                    "FACTURAR", 
                    "CONSULTAR_FACTURAS", 
                    "GESTIONAR_CLIENTES"
                );
                return permisosCajero.contains(permiso);

            default:
                return false;
        }
    }

    /**
     * Verifica si el usuario actual es ADMINISTRADOR.
     * @return true si es admin, false si es cajero o no hay sesión.
     */
    public static boolean esAdmin() {
        return loggedInUserRole == Rol.ADMINISTRADOR;
    }
}
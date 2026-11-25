# CanchApp · App móvil (Android nativo)

App para reservar las canchas del **Complejo Deportivo Calceta**: el cliente consulta la disponibilidad, reserva en bloques de 30 minutos y registra su pago; el administrador opera la agenda del día desde el celular.

Es una aplicación **nativa de Android** en **Kotlin + Jetpack Compose (Material 3)** y consume la API REST del repositorio [APP_CANCHA_BACKEND](https://github.com/cedenodaniela/APP_CANCHA_BACKEND).

## Requisitos

- Android Studio (2026.2 o superior), con el SDK de Android **API 37** y un emulador.
- JDK 17 o el que trae Android Studio.
- Para iniciar sesión contra datos reales: el backend corriendo en `http://localhost:8080` (ver su README).

## Ejecutar

1. Abre esta carpeta en Android Studio (*File → Open*). Gradle descarga las dependencias.
2. Elige un emulador y pulsa **Run**.

Desde la terminal:

```bash
./gradlew installDebug
```

La versión `debug` apunta a `http://10.0.2.2:8080/api/v1/`, que es el `localhost` de tu computadora visto desde el emulador.

**En un celular físico**, conectado por USB o con depuración inalámbrica:

1. En `local.properties` (no se versiona) agrega `canchapp.api.url=http://localhost:8080/api/v1/` y vuelve a compilar.
2. Cada vez que conectes el celular, lleva sus puertos a la computadora:

   ```bash
   adb reverse tcp:8080 tcp:8080   # la API
   adb reverse tcp:8025 tcp:8025   # Mailpit, para ver los correos en http://localhost:8025
   ```

Así el celular usa el backend de tu computadora sin depender de la IP, del firewall ni de abrir HTTP a otras direcciones. Para volver al emulador, borra esa línea de `local.properties`.

**Android 17 (API 37) o superior:** al abrir la app en debug aparece un permiso para acceder a la red local. Hay que **permitirlo**; sin él, el sistema bloquea la conexión al backend local y la app muestra «No se pudo conectar con el servidor». Si se negó, se concede así:

```bash
adb shell pm grant com.canchapp.movil android.permission.ACCESS_LOCAL_NETWORK
```

**Si Android Studio y la terminal compilan a la vez** puede salir `Storage … is already registered`: abre y compila el proyecto siempre desde la misma ruta, sin cambiar mayúsculas en el nombre de las carpetas.

### Recuperación de contraseña

El correo de recuperación trae un enlace `canchapp://restablecer-contrasena?token=…` que abre la app en la pantalla de contraseña nueva. En desarrollo, el backend deja ese correo en Mailpit: ábrelo en el emulador en `http://10.0.2.2:8025` y pulsa el botón. También se puede abrir el enlace a mano:

```bash
adb shell am start -W -a android.intent.action.VIEW -d "canchapp://restablecer-contrasena?token=EL_TOKEN" com.canchapp.movil
```

## Pruebas

```bash
./gradlew testDebugUnitTest            # unitarias (JVM)
./gradlew connectedDebugAndroidTest    # de pantalla, con un emulador abierto
./gradlew lintDebug
```

## Tecnologías

| Para qué | Biblioteca |
|---|---|
| Interfaz | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose (rutas tipadas) |
| Estado | ViewModel + StateFlow |
| Inyección de dependencias | Hilt |
| API REST | Retrofit + OkHttp + kotlinx.serialization |
| Sesión | DataStore, con el token cifrado en el Android Keystore |
| Pruebas | JUnit, MockK, MockWebServer, Compose UI Test |

## Ramas

`main` (entregas) · `calidad` (pruebas de aceptación) · `desarrollo` (integración). Cada requerimiento se trabaja en `feature/RF-XX_nombre-breve` desde `desarrollo`.

La documentación del proyecto (DERCAS, planificación y guía para agentes) vive fuera de este repositorio, en la carpeta de trabajo `app_cancha/`.

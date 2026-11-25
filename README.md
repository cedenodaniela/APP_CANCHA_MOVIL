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

La versión `debug` apunta a `http://10.0.2.2:8080/api/v1/`, que es el `localhost` de tu computadora visto desde el emulador. Para usar un celular físico, cambia `API_URL` en `app/build.gradle.kts` por la IP de tu computadora en la red local.

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

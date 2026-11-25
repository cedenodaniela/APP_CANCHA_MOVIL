# AGENTS.md — CanchApp · App móvil (APP_CANCHA_MOVIL)

App móvil **nativa de Android** del proyecto CanchApp, en **Kotlin + Jetpack Compose**. Esta es la guía **breve** de este repositorio.

> **La guía completa vive fuera de este repositorio**, en la carpeta de trabajo, un nivel arriba:
> [`../AGENTS.md`](../AGENTS.md) · DERCAS: [`../DERCAS_CanchApp.pdf`](../DERCAS_CanchApp.pdf) · plan: [`../docs/PLANIFICACION.md`](../docs/PLANIFICACION.md).
>
> Si este archivo y la guía completa se contradicen, manda la guía completa y, sobre todo, el **DERCAS**.

## Qué se construye aquí

La app del **cliente** y la administración **desde el celular**: **RF-01 a RF-17**. Solo Android, desde Android 8.0 (API 26). Nada de Ionic, Capacitor, WebView con la interfaz, Flutter ni React Native.

```
app/src/main/java/com/canchapp/movil/
├── core/       # red (Retrofit + interceptor JWT), sesión cifrada (DataStore + Keystore), navegación por rol
├── ui/         # tema institucional (theme/) y componentes comunes (componentes/)
├── auth/       # inicio de sesión, registro y recuperación (pantalla 1)
├── cliente/    # reservar/, pago/, misreservas/, avisos/
└── admin/      # agenda/, comprobantes/, canchas/, ajustes/, reportes/
```

Cada pantalla: `XxxScreen.kt` (Compose), `XxxViewModel.kt` (estado en `StateFlow`) y, en `data/`, el repositorio, la interfaz de Retrofit y sus DTOs.

## Reglas que no se rompen

- **La UI no calcula reglas de negocio.** Totales, plazos, límites y estados llegan resueltos desde la API. Si falta un dato derivado, se agrega al DTO del backend.
- Se muestra el `mensaje` de los errores `{codigo, mensaje}` del backend (`core/red/ErrorApi.kt`).
- Estados con **texto y color**. Áreas táctiles de al menos **48 dp**. Paleta en `ui/theme/Color.kt`, sin colores dinámicos.
- Fechas con `java.time` y `America/Guayaquil`. Dinero con `BigDecimal`.
- Textos en `res/values/strings.xml`, en español.
- El **pago en efectivo es solo de esta app**, no del panel web.

## Comandos

```bash
./gradlew assembleDebug           # APK de desarrollo
./gradlew installDebug            # instalarla en el emulador o celular
./gradlew testDebugUnitTest       # pruebas unitarias
./gradlew connectedDebugAndroidTest   # pruebas de pantalla (con un emulador abierto)
./gradlew lintDebug
```

En Windows: `gradlew.bat`. Se necesita JDK 17 (o el de Android Studio) y el SDK de Android con la plataforma 37. En el emulador, el backend local se ve en `http://10.0.2.2:8080`.

## Git

Ramas `main`, `calidad` y `desarrollo`. Cada RF va en `feature/RF-XX_nombre-breve` desde `desarrollo` y vuelve por Pull Request (`../docs/PLANIFICACION.md` §12). Antes de cada commit: `./gradlew testDebugUnitTest lintDebug` en verde.

## Seguridad

**Nunca** subas `google-services.json`, `local.properties` ni la clave de firma (`*.jks`, `*.keystore`).

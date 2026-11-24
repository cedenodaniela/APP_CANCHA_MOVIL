# Reglas de R8 para la versión release de CanchApp.
# kotlinx.serialization y Retrofit traen sus propias reglas de consumidor; aquí solo lo propio del proyecto.

# Los DTOs de la API se serializan por reflexión de los serializadores generados.
-keepclassmembers @kotlinx.serialization.Serializable class com.canchapp.movil.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

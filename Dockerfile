# Imagen base: Amazon Corretto 25 sobre Alpine Linux (imagen ligera)
FROM amazoncorretto:25-alpine

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el JAR generado por Maven al contenedor
# Ajusta el nombre según tu pom.xml (artifactId + version)
COPY target/rentacars-0.0.1-SNAPSHOT.jar app.jar

# Puerto que expone la aplicación (ajusta si usas otro)
EXPOSE 8080

# Comando para arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
FROM eclipse-temurin:17.0.7_7-jre as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM eclipse-temurin:17.0.7_7-jre
WORKDIR application
RUN apt-get update && apt-get install ffmpeg -y
RUN chmod 777 /application
RUN addgroup --system springboot && adduser --system springboot --ingroup springboot
USER springboot
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
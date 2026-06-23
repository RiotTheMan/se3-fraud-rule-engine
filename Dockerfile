FROM eclipse-temurin:21-jre-jammy AS runtime

ARG JAR_FILE=fraud-rule-engine-1.0.0-SNAPSHOT.jar

ENV JAR_FILE=${JAR_FILE}
ENV TZ="Africa/Johannesburg"

COPY target/${JAR_FILE} /app.jar

USER 1000

EXPOSE 8080

ENTRYPOINT exec java ${JAVA_OPTS} -jar /app.jar

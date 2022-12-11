FROM openjdk:17-jdk-alpine
VOLUME /tmp
ARG JAR_FILE=""
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
RUN wget https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O cloud_sql_proxy
RUN chmod +x cloud_sql_proxy
ENTRYPOINT ./cloud_sql_proxy -instances=error418-web-app:europe-north1:web-app-db=tcp:1433 -credential-file=/config

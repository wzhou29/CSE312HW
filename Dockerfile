FROM openjdk:8-jdk

WORKDIR /CSE312
COPY . .
VOLUME /tmp
EXPOSE 8080

CMD ["java", "-jar", "CSE312.jar", "--server.port=8080"]

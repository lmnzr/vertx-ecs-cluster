FROM adoptopenjdk/openjdk12:jre-12.33
COPY ./target/restapi-1.0-SNAPSHOT-fat.jar /app/app.jar
EXPOSE 8080
CMD java -jar /app/app.jar
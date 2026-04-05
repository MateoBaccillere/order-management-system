FROM eclipse-temurin:21-jdk

WORKDIR /app


COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

EXPOSE 8083

CMD ["java", "-jar", "target/product-service-0.0.1-SNAPSHOT.jar"]
FROM openjdk:17-jdk

WORKDIR /app

# Copy the pom.xml file
COPY pom.xml .

# Copy the mvnw files
COPY mvnw .
COPY .mvn .mvn

# Give execution rights on the mvnw file
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy the rest of your app's source code
COPY src ./src

EXPOSE ${HTTP_PORT}

CMD ["./mvnw", "clean", "compile", "exec:java"]
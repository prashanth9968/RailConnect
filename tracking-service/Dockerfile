FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY railconnect-common railconnect-common
COPY tracking-service tracking-service
RUN mvn clean package -DskipTests -pl tracking-service -am -q

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Kolkata /etc/localtime && \
    echo "Asia/Kolkata" > /etc/timezone
WORKDIR /app
RUN addgroup -S railconnect && adduser -S railconnect -G railconnect
USER railconnect
COPY --from=builder /app/tracking-service/target/*.jar app.jar
EXPOSE 8085
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

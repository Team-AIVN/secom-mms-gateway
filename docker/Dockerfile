FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y libapr1-dev libssl-dev tar gzip build-essential file && rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/*

ADD https://dlcdn.apache.org/tomcat/tomcat-connectors/native/2.0.7/source/tomcat-native-2.0.7-src.tar.gz tomcat-native.tar.gz

RUN tar -xvf tomcat-native.tar.gz && mv tomcat-native*/ tomcat-native/

WORKDIR tomcat-native/native

RUN ./configure && make && make install

FROM eclipse-temurin:21-jre

COPY --from=builder /usr/local/apr/lib /usr/lib/

# Install native libraries that can be used by Spring Boot's embedded Tomcat server for improved performance
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y libapr1 && rm -rf /var/lib/apt/lists* /var/cache/apt/archives/*

RUN mkdir /conf

COPY secom-mms-gateway-0.0.1-SNAPSHOT.jar .

CMD ["java", "-jar", "secom-mms-gateway-0.0.1-SNAPSHOT.jar", "--spring.config.location=/conf/application.yaml"]

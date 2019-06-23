FROM maven:3.6.1-jdk-8 AS build
ADD . /app
WORKDIR /app
RUN mvn clean install -pl testing,core,cli -DskipTests

FROM adoptopenjdk/openjdk8:alpine-slim
COPY --from=registry.hub.docker.com/adamcin/jvshim /go/bin/jvshim /usr/bin/java
RUN mkdir -p /app/oakpal-cli
COPY --from=build /app/cli/target/oakpal-cli-*-dist.tar.gz /app
RUN tar --strip-components 1 -C /app/oakpal-cli -zxf /app/oakpal-cli-*-dist.tar.gz \
      && rm -f /app/oakpal-cli-*-dist.tar.gz


ENV OAKPAL_PLAN "/planspace"
RUN mkdir -p "$OAKPAL_PLAN"
RUN mkdir -p /workspace
WORKDIR /workspace
ENTRYPOINT ["/app/oakpal-cli/bin/oakpal.sh"]


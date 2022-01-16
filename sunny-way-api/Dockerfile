FROM gradle:7.3.3-jdk11 AS buildApi
WORKDIR /opt
COPY gradle/ gradle/
COPY *.gradle.kts ./
COPY src/ src/
RUN gradle build --no-daemon -x test
FROM node:14-alpine AS buildUi
WORKDIR /opt
COPY ui/ ui/
RUN cd ui && npm i --production && npm run build
FROM openjdk:11-jre-slim
WORKDIR /opt
COPY --from=buildApi /opt/build/distributions/*.tar /opt/
COPY --from=buildUi /opt/ui/build/ /opt/ui/build/
RUN tar --strip-components=1 -xvf *.tar && rm *.tar
EXPOSE 8080
ENTRYPOINT [ "./bin/sunny-way-api" ]

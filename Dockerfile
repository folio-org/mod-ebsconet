# https://github.com/folio-org/folio-tools/tree/master/folio-java-docker/openjdk21
FROM folioci/alpine-jre-openjdk21:latest

# Install latest patch versions of packages: https://pythonspeed.com/articles/security-updates-in-docker/
USER root
RUN apk upgrade --no-cache

## TODO: uncomment next line
# USER folio

# Copy your fat jar to the container
ENV APP_FILE mod-ebsconet-fat.jar

# - should be a single jar file
ARG JAR_FILE=./target/*.jar
# - copy
COPY ${JAR_FILE} ${JAVA_APP_DIR}/${APP_FILE}

ARG RUN_ENV_FILE=run-env.sh

COPY ${RUN_ENV_FILE} ${JAVA_APP_DIR}/
RUN chmod 755 ${JAVA_APP_DIR}/${RUN_ENV_FILE}

# Expose this port locally in the container.
EXPOSE 8081

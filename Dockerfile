# Adopted from mozilla/sbt

# This Dockerfile has one required ARG to determine which base image
# to use for the JDK to install.

# We need JDK 8, as it does not compile with JDK 11 for some unknown reason.
ARG OPENJDK_TAG=8

# First stage just determines SBT version. If build.properties changes without changing the SBT version, it just rebuilds the first stage without rebuilding the second stage.
FROM amazoncorretto:${OPENJDK_TAG} AS sbt-version
COPY project/build.properties /
#RUN sed -n -e 's/^sbt\.version=//p' /build.properties > /version
RUN echo 1.1.1 > /version

# Second stage creates final image with the right SBT version
FROM amazoncorretto:${OPENJDK_TAG}
COPY --from=sbt-version /version /sbt-version
RUN \
  curl -L -o sbt-$(cat /sbt-version).rpm https://scala.jfrog.io/ui/api/v1/download\?repoKey=rpm\&path=%252Fsbt-$(cat /sbt-version).rpm && \
  echo SBT launcher downloaded && \
  rpm -i sbt-$(cat /sbt-version).rpm && \
  rm sbt-$(cat /sbt-version).rpm && \
  sbt sbtVersion && \
  mkdir /project
WORKDIR /project
RUN yum install -y rsync lftp nodejs zip unzip which

# Adopted from mozilla/sbt

# This Dockerfile has one required ARG to determine which base image
# to use for the JDK to install.

# We need JDK 8, as it does not compile with JDK 11 for some unknown reason.
ARG OPENJDK_TAG=8u212

# First stage just determines SBT version. If build.properties changes without changing the SBT version, it just rebuilds the first stage without rebuilding the second stage.
FROM openjdk:${OPENJDK_TAG} AS sbt-version
COPY project/build.properties /
#RUN sed -n -e 's/^sbt\.version=//p' /build.properties > /version
# The old version has some weird dependency on Java 6, so we download a newer version which will download the old version then…
RUN echo 1.1.1 > /version

# Second stage creates final image with the right SBT version
FROM openjdk:${OPENJDK_TAG}
COPY --from=sbt-version /version /sbt-version
RUN \
  curl -L -o sbt-$(cat /sbt-version).deb https://dl.bintray.com/sbt/debian/sbt-$(cat /sbt-version).deb && \
  dpkg -i sbt-$(cat /sbt-version).deb && \
  rm sbt-$(cat /sbt-version).deb && \
  sbt sbtVersion && \
  mkdir /project
WORKDIR /project
RUN apt-get update && apt-get install -y rsync lftp nodejs zip
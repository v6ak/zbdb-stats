# Adopted from mozilla/sbt

# JDK 8 is the LTS with the longest support at the time of writing.
ARG OPENJDK_TAG=8

# First stage just determines SBT version. If build.properties changes without changing the SBT version, it just rebuilds the first stage without rebuilding the second stage.
FROM amazoncorretto:${OPENJDK_TAG} AS sbt-version
COPY project/build.properties /
RUN sed -n -e 's/^sbt\.version=//p' /build.properties > /version

# Second stage creates final image with the right SBT version
FROM amazoncorretto:${OPENJDK_TAG} AS conservative
RUN yum install -y rsync lftp zip unzip which
ARG NODE_VERSION=16
RUN yum install https://rpm.nodesource.com/pub_${NODE_VERSION}.x/nodistro/repo/nodesource-release-nodistro-1.noarch.rpm -y
RUN yum install nodejs npm -y --setopt=nodesource-nodejs.module_hotfixes=1
COPY --from=sbt-version /version /sbt-version
RUN \
  curl -L -o sbt-$(cat /sbt-version).rpm https://scala.jfrog.io/ui/api/v1/download\?repoKey=rpm\&path=%252Fsbt-$(cat /sbt-version).rpm && \
  echo SBT launcher downloaded && \
  rpm -i sbt-$(cat /sbt-version).rpm && \
  rm sbt-$(cat /sbt-version).rpm && \
  mkdir /project
WORKDIR /project

# This stage can be used for testing whether the build is compatible with fresh versions of JDK and NodeJS/NPM
FROM fedora:latest AS sbt-version-bleeding-edge
COPY project/build.properties /
RUN sed -n -e 's/^sbt\.version=//p' /build.properties > /version

FROM fedora:latest AS bleeding-edge
RUN dnf install -y rsync lftp zip unzip which nodejs npm java-latest-openjdk-devel
COPY --from=sbt-version-bleeding-edge /version /sbt-version
RUN \
  curl -L -o sbt-$(cat /sbt-version).rpm https://scala.jfrog.io/ui/api/v1/download\?repoKey=rpm\&path=%252Fsbt-$(cat /sbt-version).rpm && \
  echo SBT launcher downloaded && \
  rpm -i sbt-$(cat /sbt-version).rpm && \
  rm sbt-$(cat /sbt-version).rpm && \
  mkdir /project
WORKDIR /project


FROM conservative AS default

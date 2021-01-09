FROM openjdk:11

ENV SBT_VERSION 1.4.4

RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt && \
  sbt -Dsbt.rootdir=true sbtVersion

WORKDIR /docker_folder/

COPY . /docker_folder/
EXPOSE 9000

CMD sbt run

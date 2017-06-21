FROM maven:3.5
MAINTAINER thibaut.mottet@pupscan.fr

ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /workspace
COPY . .
RUN mvn install

CMD ["java", "-jar", "./target/crawler-0.0.1-SNAPSHOT.jar"]


FROM maven:3.9-eclipse-temurin-21

WORKDIR /app
COPY pom.xml /app/pom.xml
COPY ./src /app/src
COPY ./scripts/init-app.sh /app/entrypoint.sh
COPY .env /app/.env

RUN apt-get update && apt-get install -y jq && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/data


RUN chmod +x /app/entrypoint.sh
EXPOSE 8080

CMD ["/app/entrypoint.sh"]
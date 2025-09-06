#!/bin/bash
if [ -f .env ]; then
    echo "Loading environment variables from .env file..."
    export $(grep -v '^#' .env | xargs)
    echo "Environment variables loaded successfully"
else
    echo "Warning: .env file not found, using defaults"
fi

# Mostrar variables cargadas (sin mostrar secretos)
echo "=== Configuration ==="
echo "SERVER_PORT=${ENV_SERVER_PORT}"
echo "LLM_BASEURL=${OLLAMA_BASE_URL}"
echo "MULTIAGENT_DATADIR=${DATA_DIR}"
echo "===================="

# Ejecutar Maven
exec mvn spring-boot:run -Dspring-boot.run.profiles=docker -e
# Multi Agent Service

Microservicio **Spring Boot** multi-agente con **workflows**, **memoria por usuario**, **canary de modelos** y **observabilidad**. Diseñado para integrarse con otros servicios

## ✨ Features
- Workflows reproducibles (cargar memoria → generar → validar → guardar)
- Memoria por usuario en ficheros + resumen automático
- Canary de modelos (A/B) y hot switch
- Métricas (Prometheus/Micrometer), health, logs JSON
- API key y fallbacks resilientes

## 🧩 Arquitectura (resumen)
![diagram](docs/architecture.md)

## 🚀 Quickstart
> Requiere **Ollama** corriendo en tu host: `http://localhost:11434`  
> Si Ollama está en otra máquina, ajusta `OLLAMA_BASE_URL`.
cp .env.example .env         # edita la API key 

### Docker
docker compose up --build

### Health check
curl -s http://localhost:8080/actuator/health
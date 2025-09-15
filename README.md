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

### Variables de entorno relevantes
| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| DATA_DIR | Carpeta base de datos/memorias | ./data |
| MAX_HISTORY_LINES | Límite de líneas antes de rotar historial | 800 |
| SUMMARIZATION_EVERY | Cada cuántos turnos resumir | 12 |
| OLLAMA_BASE_URL | Endpoint Ollama | http://host.docker.internal:11434 |
| LLM_TIMEOUT_MS | Timeout llamadas LLM (ms) | 5000 |
| ENV_APIKEY | API key requerida en header `x-api-key` | secret123 |

### Uso en Windows
Los scripts de `scripts/*.sh` requieren WSL, Git Bash o similar. Alternativas:
1. Ejecutar dentro del contenedor (bash instalado).
2. Crear versiones `.bat` mínimas (pendiente). Ejemplo manual descarga modelo:
```
curl -X POST %OLLAMA_BASE_URL%/api/pull -d "{\"name\":\"llama3:instruct\"}"
```

### Notas de portabilidad
* Preferir rutas relativas vía `DATA_DIR`.
* Evitar montar volúmenes en Windows con rutas con espacios.
* Para rendimiento en WSL: colocar `DATA_DIR` dentro de `/mnt/wsl/...` en lugar de `/mnt/c/` si el I/O es intensivo.

### Scripts útiles
| Script | Propósito |
|--------|----------|
| scripts/download-models.sh | Descarga modelos base en Ollama |
| scripts/init-app.sh | Inicializa estructura de carpetas y permisos |

Si algún script falla en Windows, replicar los pasos manualmente (ver contenido) o usar WSL.
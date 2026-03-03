# Multi Agent Service AI System

## Descripción

Este proyecto evoluciona de un prototipo inicial a una infraestructura de IA privada y segura, diseñada especificamente para sectores con alta sensibilidad de datos como energía, sanidad y administración púublica.

A diferencia de las implementaciones estándar de IA, este sistema utiliza un enfoque "Local-First", garantizando que el procesamiento de datos críticos nunca salga de la infraestructura controlada, cumpliendo con normativas de privacidad y seguridad.

## Arquitectura del sistema

El sistema se basa en una arquitectura de microservicios robusta utilizando Spring Boot y un stack de datos avanzado:
- **IA Orquestador:** Basado en **LangChain4j** para la gestiñon de agentes y memoria de contexto.
- **Motor de inferencia local:** Integración con **Ollama** para ejecutar modelos (Llama3/Mistral) sin dependencia de nubes externas.
- **Capa de datos híbrida:** **PostgreSQL** con **pgvector** para el manejo simultáneo de datos relacionales tradicionales y embeddings vectoriales.
- **Seguridad:** Implementación de **Spring Security** con autenticación mediante **API Keys** para entornos microservicios.
- **Observabilidad:** Sistema de auditoria persistente que registra la trazabilidad completa de consultas y SQL generado.

## Capacidades principales

1. Agente RAG (Retrieval-Augmented Generation) 
Permite realizar consultas sobre documentación técnica privada (PDFs, manuales de infraestructuras, etc)
**Tecnologia:** Procesamiento de lenguage natural mediante búsqueda semántica en vectores.

2. Agente SQL (Businness Intelligence)
Traduce lenguaje natural a consultas SQL precisas para nalizar bases de datos en tiempo real.
**Seguridad** Implementación de herramientas de solo lectura (SELECT) para proteger la integridad de datos críticos.

## Despliegue en docker
El proyecto está totalmente contenedorizado para garantizar la estabilidad entre entornos de desarrollo y producción.

### Requisitos previos
- Docker
- Docker Compose

### Comandos
```bash
# Construir y ejecutar la aplicación
docker-compose up -d

# Descargar el modelo de lenguaje
docker exec -it vargontoc-ollama ollama run llama3
```

## Decisiones de arquitectura (ADR)
* PostgreSQL + pgvector: Se priorizó la consistencia de datos y la reutilización de infraestructura existente en entornos empresariales.

* Ollama: Para eliminar la latencia de red y cumplir con las restricciones legales de soberanía de datos.

* Enfoque asíncrono: El diseño prioriza la comunicación técnica escrita y la documentación clara para facilitar el mantenimiento a largo plazo.
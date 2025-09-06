#!/bin/bash

# Start Ollama in the background.
/bin/ollama serve &
# Record Process ID.
pid=$!

# Pause for Ollama to start.
sleep 5


echo "Retrieve llama3.2:3b model..."
ollama pull llama3.2:3b
echo "Done!"

# Wait for Ollama process to finish.
wait $pid
#!/bin/bash
# Usa la ruta real de Java 21 gestionada por mise
JAVA21=$(mise which java 2>/dev/null || true)
if [ -n "$JAVA21" ]; then
  export JAVA_HOME=$(dirname $(dirname "$JAVA21"))
else
  export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
fi
export PATH=$JAVA_HOME/bin:$PATH
echo "JAVA_HOME=$JAVA_HOME"
java -version
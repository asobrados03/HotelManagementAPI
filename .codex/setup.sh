mkdir -p .codex
cat > .codex/setup.sh << 'EOF'
#!/bin/bash
# Fuerza Java 21 para compatibilidad con Gradle 8.x
export JAVA_HOME=$(update-java-alternatives -l 2>/dev/null | grep "21" | awk '{print $3}' | head -1)
if [ -z "$JAVA_HOME" ]; then
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
fi
export PATH=$JAVA_HOME/bin:$PATH
echo "Using Java: $(java -version 2>&1 | head -1)"
EOF
chmod +x .codex/setup.sh
git add .codex/setup.sh
git commit -m "chore: add codex setup script to enforce Java 21"
git push origin main
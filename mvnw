#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
MVN_CMD=""

if [ -d "${HOME}/.m2/wrapper/dists" ]; then
  MVN_CMD=$(find "${HOME}/.m2/wrapper/dists" -path '*/apache-maven-*/bin/mvn' -type f 2>/dev/null | head -n 1 || true)
fi

if [ -z "$MVN_CMD" ] && [ -x "/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.3/plugins/maven/lib/maven3/bin/mvn" ]; then
  MVN_CMD="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.3/plugins/maven/lib/maven3/bin/mvn"
fi

if [ -z "$MVN_CMD" ]; then
  echo "Could not find a local Maven installation for mvnw." >&2
  echo "Checked ~/.m2/wrapper/dists and IntelliJ IDEA bundled Maven." >&2
  exit 1
fi

exec "$MVN_CMD" -f "$SCRIPT_DIR/pom.xml" "$@"

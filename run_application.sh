#!/bin/bash

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"

run_ui() {
  cd "$PROJECT_ROOT/ui" || { echo "UI directory not found"; exit 1; }
  if (echo >/dev/tcp/localhost/8080) &>/dev/null ; then
    echo "Port 8080 is already in use. Do you want to kill the process using port 8080? (y/n)"
    read -r choice
    case "$choice" in
      y|Y )
        # Kill the process using port 8080
        npx kill-port 8080
        ;;
      * )
        echo "Exiting..."
        echo "Exited with 1"
        exit 1
        ;;
    esac
  fi

  echo "Running npm install on UI..."
  npm install --legacy-peer-deps
  echo "Starting UI server"
  npm run start &
  echo "UI is running on port 8080"
  echo "For more details and all required versions, see UI Documentation."
}

run_api() {
  cd "$PROJECT_ROOT/api" || { echo "API directory not found"; exit 1; }

  ./mvnw spring-boot:run -Dspring-boot.run.profiles=local &
  echo "API is starting on port 8082 with embedded H2 and local-only test credentials"
  echo "For more details and all required versions, see API Documentation."
}

main() {
  echo "Running the application locally..."
  run_api
  run_ui
}

# Run the main script
main

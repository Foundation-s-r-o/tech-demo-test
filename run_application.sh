#!/bin/bash

setup_env() {
  if [ ! -f .env ]; then
    echo "Please copy .env - Sample into .env file and fill in missing values."
    echo "Exiting..."
    exit 1
  else
    echo ".env file already exists. Please secure that you used the correct values"
  fi
}

run_ui() {
  cd ../ui || { echo "UI directory not found"; exit 1; }
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
  npm install
  echo "Starting UI server"
  npm run start &
  echo "UI is running on port 8080"
  echo "For more details and all required versions, see UI Documentation."
}

run_api() {
  cd ./api || { echo "API directory not found"; exit 1; }

  # Prompt the user for database username and password
  read -r -p "Enter the database username (press Enter to use .env values): " db_user
  read -r -s -p "Enter the database password (press Enter to use .env values): " db_pwd
  echo

  # Retrieve database username and password from .env file if available
  source ../.env
  # Use default values if not provided by the user or in the .env file
  db_user="${db_user:-$MYSQL_DATABASE_USERNAME}"
  db_pwd="${db_pwd:-$MYSQL_DATABASE_PASSWORD}"

  ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://localhost:3306/tech-demo --spring.datasource.username=$db_user --spring.datasource.password=$db_pwd" &
  echo "API is running"
  echo "For more details and all required versions, see API Documentation."
}

run_docker() {
  docker-compose up --build -d
  echo "Application is running in Docker"
}

main() {
  read -r -p "How do you want to run the application? (l: locally, d: in Docker, b: both) " choice
  case "$choice" in
    l|L )
      echo "Running the application locally..."
      setup_env
      run_api
      run_ui
      ;;
    d|D )
      echo "Running the application in Docker..."
      setup_env
      run_docker
      ;;
    b|B )
      echo "Running the application locally and in Docker..."
      setup_env
      run_api
      run_ui
      run_docker
      ;;
    * )
      echo "Invalid input. Exiting..."
      ;;
  esac
}

# Run the main script
main

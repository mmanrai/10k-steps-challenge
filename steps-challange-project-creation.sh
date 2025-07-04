#!/bin/bash

# Create project directory
echo "Creating steps-challenge project..."
mkdir -p steps-challenge
cd steps-challenge

# Initialize with Gradle (gets wrapper and basic files)
echo "Initializing Gradle project..."
gradle init --type basic --dsl kotlin --project-name steps-challenge

# Create gradle directory for version catalog
mkdir -p gradle

# Create module directories with proper Java package structure
echo "Creating module directories..."

modules=(
    "user-profile-service/userprofile"
    "activity-service/activity" 
    "ingestion-service/ingestion"
    "congrats-service/congrats"
    "event-stats-service/stats"
    "public-api/api"
    "user-webapp/webapp/user"
    "dashboard-webapp/webapp/dashboard"
)

for module_info in "${modules[@]}"; do
    IFS='/' read -ra PARTS <<< "$module_info"
    module_name="${PARTS[0]}"
    package_path="${PARTS[@]:1}"
    
    # Create main Java directories
    mkdir -p "${module_name}/src/main/java/com/stepschallenge/${package_path// /\/}"
    mkdir -p "${module_name}/src/test/java/com/stepschallenge/${package_path// /\/}"
    
    # Create resources
    mkdir -p "${module_name}/src/main/resources"
    mkdir -p "${module_name}/src/test/resources"
    
    # Create webroot for web modules
    if [[ $module_name == *"webapp"* ]] || [[ $module_name == "public-api" ]]; then
        mkdir -p "${module_name}/src/main/resources/webroot"
        mkdir -p "${module_name}/src/main/resources/webroot/css"
        mkdir -p "${module_name}/src/main/resources/webroot/js"
        mkdir -p "${module_name}/src/main/resources/webroot/assets"
    fi
    
    echo "Created module: ${module_name}"
done

echo ""
echo "Project structure created successfully!"
echo "Next steps:"
echo "1. Replace the generated build.gradle.kts with your custom one"
echo "2. Replace settings.gradle.kts with your module configuration" 
echo "3. Add gradle/libs.versions.toml for version catalog"
echo "4. Run './gradlew build' to verify setup"
echo ""
echo "Gradle wrapper is ready at ./gradlew"

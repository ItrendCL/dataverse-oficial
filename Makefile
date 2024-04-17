clean-build:
	echo "Building container-base" 
	mvn -Pitrend clean package -f modules/container-base/pom.xml
	echo "Building container-web"
	mvn -Pitrend clean package -f pom.xml

base:
	echo "Building container-base" 
	mvn -Pitrend package -f modules/container-base/pom.xml

app:
	echo "Building container-web"
	mvn -Pitrend package -f pom.xml

full:
	echo "Building container-base" 
	mvn -Pitrend clean package -f modules/container-base/pom.xml
	echo "Building container-web"
	mvn -Pitrend clean package -f pom.xml

start:
	echo "Running docker compose"
	docker-compose -f docker-compose-dev.yml up
	
dev: 
	echo "Building container-base" 
	mvn -Pdev clean package -f modules/container-base/pom.xml
	echo "Building container-web"
	mvn -Pdev clean package -f pom.xml
	echo "Running container-web"
	mvn -Pdev docker:start -f pom.xml

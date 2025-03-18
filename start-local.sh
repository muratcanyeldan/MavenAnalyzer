#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting Docker services (UI, MySQL, Redis)...${NC}"
docker compose up -d --build

echo -e "${YELLOW}Waiting for MySQL to initialize...${NC}"
sleep 10

echo -e "${YELLOW}Starting backend locally with Maven...${NC}"
echo -e "${GREEN}Backend will use MySQL and Redis from Docker...${NC}"

if [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac OS X
    osascript -e 'tell app "Terminal" to do script "cd '$PWD' && mvn spring-boot:run -Dspring.datasource.url=jdbc:mysql://localhost:3306/maven_analyzer -Dspring.datasource.username=root -Dspring.datasource.password=root -Dspring.redis.host=localhost"'
    echo -e "${GREEN}Backend started in a new terminal window!${NC}"
else
    # Linux or other
    echo -e "${YELLOW}Starting backend in current terminal...${NC}"
    mvn spring-boot:run \
        -Dspring.datasource.url=jdbc:mysql://localhost:3306/maven_analyzer \
        -Dspring.datasource.username=root \
        -Dspring.datasource.password=root \
        -Dspring.redis.host=localhost
fi

echo -e "${GREEN}Local development environment is up and running!${NC}"
echo -e "${GREEN}Frontend: http://localhost:3000${NC}"
echo -e "${GREEN}Backend: http://localhost:8080/api${NC}"
echo -e "${GREEN}MySQL: localhost:3306${NC}"
echo -e "${GREEN}Redis: localhost:6379${NC}" 
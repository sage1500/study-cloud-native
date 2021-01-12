@echo off
set base=%~d0%~p0

docker-compose -f %base%scndb\docker-compose.yml up -d
docker-compose -f %base%scnactivemq\docker-compose.yml up -d
docker-compose -f %base%scnredis\docker-compose.yml up -d
docker-compose -f %base%scnkeycloak\docker-compose.yml up -d

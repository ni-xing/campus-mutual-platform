@echo off
chcp 65001 >nul
title Campus Mutual Platform - container status and memory baseline
cd /d "%~dp0..\docker"

echo ============ 容器状态 ============
docker compose -f docker-compose.yml -f docker-compose.obs.yml --profile observability ps

echo.
echo ============ 内存占用（W0 基线，对照生产 2.88GB 预算）============
docker stats --no-stream --format "table {{.Name}}	{{.MemUsage}}	{{.CPUPerc}}"

echo.
echo 导出基线到 docker/data/w0-memory-baseline.txt ...
if not exist data mkdir data
docker stats --no-stream --format "{{.Name}},{{.MemUsage}},{{.CPUPerc}}" > data\w0-memory-baseline.txt
echo 已写入 docker/data/w0-memory-baseline.txt
pause

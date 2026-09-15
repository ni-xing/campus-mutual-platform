@echo off
chcp 65001 >nul
title Campus Mutual Platform - observability profile
cd /d "%~dp0..\docker"

echo 启动观测组合（prometheus / grafana / zipkin / loki，约 800MB，按需启动）...
docker compose -f docker-compose.yml -f docker-compose.obs.yml --profile observability up -d
if errorlevel 1 (
  echo.
  echo [ERROR] 启动失败：请先确认 Docker Desktop 已运行。
  pause
  exit /b 1
)
echo.
echo   Grafana    http://127.0.0.1:3000  （账号密码见 docker/.env）
echo   Prometheus http://127.0.0.1:9090
echo   Zipkin     http://127.0.0.1:9411
echo.
echo 注意：压测时不要与观测组合同时全开（《系统设计》§5.5.4）
pause

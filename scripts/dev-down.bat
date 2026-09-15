@echo off
chcp 65001 >nul
title Campus Mutual Platform - dev down
cd /d "%~dp0..\docker"

echo 停止全部容器（数据卷保留，不丢数据）...
docker compose -f docker-compose.yml -f docker-compose.obs.yml --profile observability down
echo.
echo 如需彻底重置数据库（删除所有数据卷，下次启动重新执行 DDL）：
echo   docker compose -f docker-compose.yml down -v
pause

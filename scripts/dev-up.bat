@echo off
chcp 65001 >nul
title Campus Mutual Platform - dev up
cd /d "%~dp0..\docker"

echo [1/3] 启动常驻组合（mysql / redis / nacos / sentinel / nginx）...
docker compose up -d
if errorlevel 1 (
  echo.
  echo [ERROR] 启动失败：请先确认 Docker Desktop 已运行（任务栏鲸鱼图标为绿色）。
  pause
  exit /b 1
)

echo.
echo [2/3] 等待中间件就绪（最多 120 秒）...
timeout /t 20 /nobreak >nul
docker compose ps

echo.
echo [3/3] 服务地址（本地 dev，均绑 127.0.0.1）：
echo   Nacos 控制台    http://127.0.0.1:8848/nacos  （账号密码见 docker/.env）
echo   Sentinel 控制台 http://127.0.0.1:8858       （账号密码见 docker/.env）
echo   Nginx 入口      http://127.0.0.1:8088/healthz
echo   MySQL          127.0.0.1:3306（7 个库）
echo   Redis          127.0.0.1:6379
echo.
echo 运行 dev-status.bat 查看内存占用（W0 基线）
pause

@echo off
echo ⚛️ Starting Unified Quantum Auth SDK Setup for Windows...

:: 1. Install Java Starter
echo 📦 Installing Java Quantum-JWT Starter...
cd java\quantum-jwt-starter
call mvn clean install -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Maven install failed. Ensure Maven and Java are in your PATH.
    pause
    exit /b %ERRORLEVEL%
)
cd ..\..

:: 2. Setup Python Miner (Only if not using Docker)
echo 🐍 Setting up Python Quantum Miner (Local)...
cd quantum-miner
python -m venv venv
call venv\Scripts\activate
pip install -r requirements.txt
cd ..

echo ------------------------------------------------
echo ✅ SDK Setup Complete!
echo 1. Start Infrastructure: Run 'docker-compose up -d'
echo 2. Run Demo: Go to 'java/examples/spring-boot-app' and run 'mvn spring-boot:run'
echo ------------------------------------------------
pause

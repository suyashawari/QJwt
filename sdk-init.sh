#!/bin/bash

# 🚀 Quantum Auth SDK - Unified Setup Script
echo "⚛️ Starting Unified Quantum Auth SDK Setup..."

# 1. Install Java Starter
echo "📦 Installing Java Quantum-JWT Starter..."
cd java/quantum-jwt-starter
mvn clean install -DskipTests
cd ../..

# 2. Setup Python Miner
echo "🐍 Setting up Python Quantum Miner..."
cd quantum-miner
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
cd ..

# 3. Final Success Message
echo "------------------------------------------------"
echo "✅ SDK Setup Complete!"
echo "1. Start Redis: 'docker-compose up -d' or 'redis-server'"
echo "2. Start Miner: 'cd quantum-miner && source venv/bin/activate && python3 miner.py'"
echo "3. Use in Java: Add 'quantum-jwt-starter' dependency to your pom.xml"
echo "------------------------------------------------"

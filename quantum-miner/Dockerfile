# Use a slim Python image for a small footprint
FROM python:3.9-slim

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

WORKDIR /app

# Install dependencies
# Note: qiskit and qiskit-ibm-runtime are heavy, so we cache the installation
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the miner code
COPY . .

# Run the miner
CMD ["python", "miner.py"]
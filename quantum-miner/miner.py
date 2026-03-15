import os
import time
import logging
import sys
import threading
from dotenv import load_dotenv

# Load .env BEFORE any project imports
load_dotenv()

from config import settings
from redis_connector import RedisClient
from qng_source import QuantumEntropySource

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("MinerMain")

# Debug: check if token is loaded
if settings.ibm_quantum_token:
    logger.info(f"IBM Quantum token found (starts with: {settings.ibm_quantum_token[:5]}...)")
else:
    logger.warning("IBM Quantum token not found in environment.")

# Global state
quantum_available = False
quantum_source = None
redis_client = None

def quantum_retry_loop():
    """Background thread that periodically tries to reconnect to IBM Quantum."""
    global quantum_available, quantum_source
    while True:
        if not quantum_available:
            logger.info("Background: Attempting to reconnect to IBM Quantum...")
            new_source = QuantumEntropySource()
            if new_source.is_quantum_available():
                quantum_available = True
                quantum_source = new_source
                logger.info("Background: Quantum connection re-established.")
            else:
                logger.debug("Background: Quantum still unavailable.")
        time.sleep(60)  # retry every 60 seconds

def main():
    global quantum_available, quantum_source, redis_client

    logger.info("Starting Quantum Entropy Miner...")
    redis_client = RedisClient()
    redis_client.wait_for_connection()

    # Initial quantum source attempt
    quantum_source = QuantumEntropySource()
    quantum_available = quantum_source.is_quantum_available()

    # Start background retry thread
    retry_thread = threading.Thread(target=quantum_retry_loop, daemon=True)
    retry_thread.start()

    while True:
        try:
            current_size = redis_client.get_pool_size()
            logger.info(f"Pool size: {current_size}")

            if current_size < settings.min_pool_size:
                needed = settings.max_pool_size - current_size
                batch_count = min(needed, settings.batch_size)
                logger.warning(f"Pool low ({current_size} < {settings.min_pool_size}). Mining {batch_count} keys...")

                keys = []
                source = "classical"

                if quantum_available:
                    try:
                        # Modified: only set source to quantum if the batch is successful
                        # Note: qng_source.py currently falls back internally. 
                        # We should check if quantum succeeded.
                        if quantum_source.is_quantum_available():
                            keys = quantum_source._generate_quantum_entropy(batch_count)
                            source = "quantum"
                            logger.info(f"✅ Generated {len(keys)} keys via IBM Quantum Hardware")
                    except Exception as e:
                        logger.error(f"⚠️ Quantum job failed: {e}")
                        logger.info("Falling back to classical for this batch.")
                        quantum_available = False
                
                if not keys:  # quantum failed or not available
                    keys = quantum_source.get_classical_batch(batch_count, settings.key_size_bytes)
                    source = "classical"
                    logger.info(f"🔐 Generated {len(keys)} keys via classical CSPRNG")

                redis_client.push_keys(keys, source)
            else:
                time.sleep(2)
        except Exception as e:
            logger.critical(f"Miner crashed: {e}")
            time.sleep(5)

if __name__ == "__main__":
    main()
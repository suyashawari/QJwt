# import os
# import time
# import secrets
# import logging
# import random
# from datetime import datetime
# from typing import List, Tuple
# import redis
# from dotenv import load_dotenv

# # --- QISKIT IMPORTS (MATCHING YOUR WORKING CODE) ---
# from qiskit import QuantumCircuit, transpile
# from qiskit_ibm_runtime import QiskitRuntimeService, SamplerV2 as Sampler

# # Setup Logging
# logging.basicConfig(
#     format='%(asctime)s [%(levelname)s] %(message)s',
#     datefmt='%Y-%m-%d %H:%M:%S',
#     level=logging.INFO
# )
# logger = logging.getLogger('quantum-miner')

# # Load Environment Variables
# load_dotenv()

# # --- CONFIGURATION ---
# # !!! MAKE SURE YOUR .env FILE HAS THIS VARIABLE !!!
# IBM_QUANTUM_TOKEN = os.getenv('IBM_QUANTUM_TOKEN')

# REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
# REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
# MIN_POOL_SIZE = int(os.getenv('MIN_POOL_SIZE', 1000))
# BATCH_SIZE = int(os.getenv('BATCH_SIZE', 100)) # Matched your script quantity
# HONEY_TOKEN_FREQUENCY = int(os.getenv('HONEY_TOKEN_FREQUENCY', 500))

# # Global Variables for Quantum Service
# service = None
# backend = None
# _qiskit_available = False

# # ---------------------------------------------------------
# # STEP 1 & 2: SETUP (From your code)
# # ---------------------------------------------------------
# def init_qiskit():
#     global service, backend, _qiskit_available

#     if not IBM_QUANTUM_TOKEN:
#         logger.warning("⚠️  IBM_QUANTUM_TOKEN is missing in .env file!")
#         return False

#     logger.info("🔌 Connecting to IBM Cloud...")
    
#     try:
#         # Connect to the service
#         service = QiskitRuntimeService(channel="ibm_quantum_platform", token=IBM_QUANTUM_TOKEN)
        
#         # Select Backend (Using your logic)
#         target_machine = "ibm_marrakesh"
#         try:
#             backend = service.backend(target_machine)
#             logger.info(f"✅ Successfully connected to: {backend.name}")
#         except:
#             logger.warning(f"⚠️ '{target_machine}' not found. Auto-selecting least busy...")
#             backend = service.least_busy(operational=True, simulator=False)
#             logger.info(f"✅ Connected to: {backend.name}")
            
#         _qiskit_available = True
#         return True

#     except Exception as e:
#         logger.error(f"❌ Connection Error: {e}")
#         _qiskit_available = False
#         return False

# # ---------------------------------------------------------
# # STEP 3: MINING FUNCTION (Your exact logic)
# # ---------------------------------------------------------
# def generate_quantum_keys(count: int) -> List[str]:
#     logger.info(f"🏗️  Building Quantum Circuit to mine {count} keys...")

#     # 128 Qubits = 128 bits of entropy
#     num_qubits = 128
    
#     # Adjust qubits if backend is smaller (safety check)
#     if backend.num_qubits < 128:
#         num_qubits = backend.num_qubits
#         logger.warning(f"⚠️ Backend only has {num_qubits} qubits. Adjusting circuit.")

#     qc = QuantumCircuit(num_qubits)
#     qc.h(range(num_qubits))       # Superposition
#     qc.measure_all()              # Collapse

#     # --- CRITICAL FIX: TRANSPILATION ---
#     logger.info(f"🔧 Transpiling circuit for {backend.name} hardware...")
#     transpiled_qc = transpile(qc, backend=backend)
#     # -----------------------------------

#     logger.info(f"🚀 Sending Job to {backend.name} (Requesting {count} shots)...")
    
#     try:
#         sampler = Sampler(backend)
#         # Using the new SamplerV2 syntax
#         job = sampler.run([(transpiled_qc, None, count)])
        
#         logger.info(f"⏳ Job submitted (ID: {job.job_id()}). Waiting for physics...")
        
#         result = job.result()
#         pub_result = result[0]
#         bitstrings = pub_result.data.meas.get_bitstrings()

#         logger.info(f"✅ Data Received! Processing {len(bitstrings)} keys...")

#         final_keys = []
#         for bits in bitstrings:
#             # Convert binary to hex
#             hex_key = hex(int(bits, 2))[2:]
#             final_keys.append(hex_key)
            
#         return final_keys

#     except Exception as e:
#         logger.error(f"❌ Quantum Job Failed: {e}")
#         raise e

# # ---------------------------------------------------------
# # REDIS INTEGRATION
# # ---------------------------------------------------------
# def connect_redis():
#     return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

# def push_to_redis(r, keys, source):
#     pipeline = r.pipeline()
#     for key in keys:
#         # Honey Token Logic
#         if random.randint(1, HONEY_TOKEN_FREQUENCY) == 1:
#             poison = f"POISON_{secrets.token_hex(30)}"
#             pipeline.rpush('entropy_pool', poison)
#             pipeline.sadd('poison_keys', poison)
            
#         pipeline.rpush('entropy_pool', key)
    
#     # Update Stats
#     pipeline.hincrby('miner_stats', 'total_keys_generated', len(keys))
#     pipeline.hincrby('miner_stats', f'keys_from_{source}', len(keys))
#     pipeline.hset('miner_stats', 'last_generation', datetime.utcnow().isoformat())
#     pipeline.execute()

# # ---------------------------------------------------------
# # MAIN LOOP
# # ---------------------------------------------------------
# def main():
#     # Initialize Quantum
#     if init_qiskit():
#         logger.info("✅ PHASE 1 COMPLETE: Quantum Source is Active.")
#     else:
#         logger.error("❌ Quantum Init Failed. Will use Classical Fallback.")

#     r = connect_redis()

#     while True:
#         try:
#             current_size = r.llen('entropy_pool')
#             logger.info(f"📈 Pool Size: {current_size} / {MIN_POOL_SIZE}")

#             if current_size < MIN_POOL_SIZE:
#                 deficit = MIN_POOL_SIZE - current_size
#                 # Don't ask for too many at once from quantum (it takes time)
#                 count = min(BATCH_SIZE, deficit) 

#                 if _qiskit_available:
#                     try:
#                         keys = generate_quantum_keys(count)
#                         push_to_redis(r, keys, 'quantum')
#                         logger.info(f"⚛️  Refilled pool with {len(keys)} QUANTUM keys")
#                     except Exception as e:
#                         logger.error("⚠️ Quantum failed temporarily. Using classical fallback.")
#                         keys = [secrets.token_hex(32) for _ in range(count)]
#                         push_to_redis(r, keys, 'classical')
#                 else:
#                     keys = [secrets.token_hex(32) for _ in range(count)]
#                     push_to_redis(r, keys, 'classical')
#                     logger.info(f"🔐 Refilled pool with {len(keys)} classical keys")

#             else:
#                 time.sleep(10) # Sleep if pool is full

#         except Exception as e:
#             logger.error(f"Error in main loop: {e}")
#             time.sleep(5)

# if __name__ == '__main__':
#     main()

import os
import time
import secrets
import logging
import random
from datetime import datetime
from typing import List
import redis
from dotenv import load_dotenv

# --- QISKIT IMPORTS ---
from qiskit import QuantumCircuit, transpile
from qiskit_ibm_runtime import QiskitRuntimeService, SamplerV2 as Sampler

# Setup Logging
logging.basicConfig(
    format='%(asctime)s [%(levelname)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    level=logging.INFO
)
logger = logging.getLogger('quantum-miner')

# Load Environment Variables
load_dotenv()

# --- CONFIGURATION ---
IBM_QUANTUM_TOKEN = os.getenv('IBM_QUANTUM_TOKEN')
REDIS_HOST = os.getenv('REDIS_HOST', 'localhost')
REDIS_PORT = int(os.getenv('REDIS_PORT', 6379))
BATCH_SIZE = int(os.getenv('BATCH_SIZE', 1024)) 
HONEY_TOKEN_FREQUENCY = int(os.getenv('HONEY_TOKEN_FREQUENCY', 500))

# We only refill if the pool drops below this number
Refill_Threshold = 100 

service = None
backend = None
_qiskit_available = False

# ---------------------------------------------------------
# SETUP
# ---------------------------------------------------------
def init_qiskit():
    global service, backend, _qiskit_available
    if not IBM_QUANTUM_TOKEN:
        logger.warning("⚠️  IBM Token missing.")
        return False
    
    logger.info("🔌 Connecting to IBM Cloud...")
    try:
        service = QiskitRuntimeService(channel="ibm_quantum_platform", token=IBM_QUANTUM_TOKEN)
        # Force use of the fast machine you found
        target_machine = "ibm_fez"
        try:
            backend = service.backend(target_machine)
            logger.info(f"✅ Connected to fast lane: {backend.name}")
        except:
            backend = service.least_busy(operational=True, simulator=False)
            logger.info(f"⚠️ {target_machine} busy, using: {backend.name}")
            
        _qiskit_available = True
        return True
    except Exception as e:
        logger.error(f"❌ Quantum Connection Error: {e}")
        _qiskit_available = False
        return False

# ---------------------------------------------------------
# MINING LOGIC
# ---------------------------------------------------------
def generate_quantum_keys(count: int) -> List[str]:
    logger.info(f"🏗️  Building Quantum Circuit for {count} keys...")
    
    # 1. Create Circuit
    num_qubits = min(127, backend.num_qubits) # Safety limit
    qc = QuantumCircuit(num_qubits)
    qc.h(range(num_qubits))
    qc.measure_all()

    # 2. Transpile
    logger.info(f"🔧 Transpiling for {backend.name}...")
    transpiled_qc = transpile(qc, backend=backend)

    # 3. Run Job
    logger.info(f"🚀 Sending Job to IBM (Requesting {count} shots)...")
    sampler = Sampler(backend)
    job = sampler.run([(transpiled_qc, None, count)])
    
    logger.info(f"⏳ Job submitted (ID: {job.job_id()}). Waiting...")
    
    # BLOCKING WAIT - This prevents multiple requests!
    result = job.result()
    
    # 4. Process Results
    bitstrings = result[0].data.meas.get_bitstrings()
    logger.info(f"✅ Received {len(bitstrings)} quantum states.")
    
    final_keys = []
    for bits in bitstrings:
        final_keys.append(hex(int(bits, 2))[2:])
        
    return final_keys

# ---------------------------------------------------------
# REDIS OPERATIONS (FLUSH AND FILL)
# ---------------------------------------------------------
def connect_redis():
    return redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True)

def replace_pool_with_keys(r, keys, source):
    """
    This function deletes the old pool and puts in the new keys.
    This guarantees we never overflow.
    """
    pipeline = r.pipeline()
    
    # 1. CLEAR OLD DATA
    pipeline.delete('entropy_pool')
    pipeline.delete('poison_keys') 
    
    # 2. INSERT NEW KEYS
    poison_count = 0
    for key in keys:
        # Honey Token Logic
        if random.randint(1, HONEY_TOKEN_FREQUENCY) == 1:
            poison = f"POISON_{secrets.token_hex(30)}"
            pipeline.rpush('entropy_pool', poison)
            pipeline.sadd('poison_keys', poison)
            poison_count += 1
            
        pipeline.rpush('entropy_pool', key)
    
    # 3. UPDATE STATS
    pipeline.hincrby('miner_stats', 'total_keys_generated', len(keys))
    pipeline.hincrby('miner_stats', f'keys_from_{source}', len(keys))
    pipeline.hset('miner_stats', 'last_generation', datetime.utcnow().isoformat())
    pipeline.hset('miner_stats', 'honey_tokens_injected', poison_count)
    
    pipeline.execute()
    logger.info(f"🔄 Pool FLUSHED and REFILLED with {len(keys)} {source} keys (Poison: {poison_count})")

# ---------------------------------------------------------
# MAIN EXECUTION
# ---------------------------------------------------------
def main():
    r = connect_redis()
    
    # STARTUP: Clear everything to start fresh
    logger.info("🧹 Startup: Clearing Redis Pool...")
    r.delete('entropy_pool')
    
    init_qiskit()

    while True:
        try:
            current_size = r.llen('entropy_pool')
            
            # ONLY Mine if pool is VERY low (prevents over-fetching)
            if current_size < Refill_Threshold:
                logger.info(f"📉 Pool low ({current_size} keys). Initiating refill sequence...")
                
                keys = []
                source = 'classical'
                
                # Try Quantum First
                if _qiskit_available:
                    try:
                        keys = generate_quantum_keys(BATCH_SIZE)
                        source = 'quantum'
                    except Exception as e:
                        logger.error(f"⚠️ Quantum failed: {e}. Falling back...")
                        keys = [secrets.token_hex(32) for _ in range(BATCH_SIZE)]
                        source = 'classical'
                else:
                    keys = [secrets.token_hex(32) for _ in range(BATCH_SIZE)]
                
                # Critical: Replace the pool
                replace_pool_with_keys(r, keys, source)

            else:
                # If pool is healthy, just wait.
                # We print every 30 seconds to show we are alive.
                logger.info(f"✅ Pool Healthy ({current_size} keys). Standing by...")
                time.sleep(30)

        except KeyboardInterrupt:
            logger.info("👋 Stopping Miner...")
            break
        except Exception as e:
            logger.error(f"❌ System Error: {e}")
            time.sleep(5)

if __name__ == '__main__':
    main()
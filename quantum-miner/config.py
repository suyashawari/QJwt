import os
from typing import Optional
from pydantic import BaseModel

class Config(BaseModel):
    redis_host: str = os.getenv("REDIS_HOST", "localhost")
    redis_port: int = int(os.getenv("REDIS_PORT", 6379))
    redis_password: str = os.getenv("REDIS_PASSWORD", "")
    min_pool_size: int = int(os.getenv("MIN_POOL_SIZE", 50))
    max_pool_size: int = int(os.getenv("MAX_POOL_SIZE", 500))
    batch_size: int = int(os.getenv("BATCH_SIZE", 10))
    key_size_bytes: int = int(os.getenv("KEY_SIZE_BYTES", 32))
    ibm_quantum_token: Optional[str] = os.getenv("IBM_QUANTUM_TOKEN", None)
    ibm_backend_name: str = os.getenv("IBM_BACKEND_NAME", "ibmq_qasm_simulator")

settings = Config()
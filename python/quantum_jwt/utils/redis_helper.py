import base64
from typing import Optional
import redis
from ..exceptions import EntropyExhaustedError

class RedisHelper:
    """Low‑level Redis operations for entropy pool."""

    ENTROPY_POOL = "entropy:pool"

    def __init__(self, redis_client: redis.Redis):
        self.redis = redis_client

    def pop_key(self) -> Optional[bytes]:
        """
        Pop one key from the entropy pool and return raw bytes.
        Returns None if pool is empty.
        """
        key_b64 = self.redis.lpop(self.ENTROPY_POOL)
        if key_b64 is None:
            return None
        return base64.urlsafe_b64decode(key_b64 + "==")  # add padding
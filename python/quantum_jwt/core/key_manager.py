import uuid
import base64
from typing import Optional
from datetime import timedelta
import redis

class KeyManager:
    """Manages temporary storage of signing keys in Redis."""

    KEY_PREFIX = "key:"

    def __init__(self, redis_client: redis.Redis):
        self.redis = redis_client

    def store_key(self, key_bytes: bytes, ttl: timedelta) -> str:
        """
        Store a key under a new key ID and return the ID.
        """
        kid = str(uuid.uuid4())
        redis_key = self.KEY_PREFIX + kid
        key_b64 = base64.urlsafe_b64encode(key_bytes).decode().rstrip("=")
        self.redis.setex(redis_key, int(ttl.total_seconds()), key_b64)
        return kid

    def get_key(self, kid: str) -> Optional[bytes]:
        """
        Retrieve a key by its ID. Returns None if not found (expired or never existed).
        """
        redis_key = self.KEY_PREFIX + kid
        key_b64 = self.redis.get(redis_key)
        if key_b64 is None:
            return None
        return base64.urlsafe_b64decode(key_b64 + "==")  # add padding
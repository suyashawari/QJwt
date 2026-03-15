import redis
import logging
import time
from config import settings

logger = logging.getLogger("RedisConnector")

class RedisClient:
    def __init__(self):
        connection_kwargs = {
            "host": settings.redis_host,
            "port": settings.redis_port,
            "decode_responses": True,
        }
        if settings.redis_password:
            connection_kwargs["username"] = "miner"
            connection_kwargs["password"] = settings.redis_password

        self.pool = redis.ConnectionPool(**connection_kwargs)
        self.client = redis.Redis(connection_pool=self.pool)

    def get_pool_size(self) -> int:
        try:
            return self.client.llen("entropy:pool")
        except redis.RedisError as e:
            logger.error(f"Redis connection failed: {e}")
            return 9999

    def push_keys(self, keys: list[str], source: str = "unknown"):
        if not keys:
            return
        try:
            pipeline = self.client.pipeline()
            pipeline.rpush("entropy:pool", *keys)
            pipeline.hincrby("miner_stats", f"keys_from_{source}", len(keys))
            pipeline.execute()
            logger.info(f"Pushed {len(keys)} {source} keys to entropy:pool")
        except redis.RedisError as e:
            logger.error(f"Failed to push keys: {e}")

    def wait_for_connection(self):
        while True:
            try:
                self.client.ping()
                logger.info("Connected to Redis successfully.")
                break
            except redis.RedisError:
                logger.warning("Waiting for Redis...")
                time.sleep(2)
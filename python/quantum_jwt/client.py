import json
import time
import base64
import hashlib
import hmac
from typing import Optional, Dict, Any
from datetime import datetime, timedelta, timezone
import redis
from .exceptions import EntropyExhaustedError, InvalidTokenError, HoneyTokenError
from .utils.redis_helper import RedisHelper
from .utils.watermark import compute_watermark
from .core.validator import TokenValidator
from .core.key_manager import KeyManager

class QuantumJwtClient:
    """
    Main client for generating and validating Quantum JWT tokens.
    """

    def __init__(
        self,
        redis_host: str = "localhost",
        redis_port: int = 6379,
        redis_password: str = "",
        redis_db: int = 0,
        token_ttl_seconds: int = 900,  # 15 minutes
        issuer: str = "quantum-auth",
        ip_binding_enabled: bool = True,
        watermark_secret: Optional[str] = None,
    ):
        self.redis_client = redis.Redis(
            host=redis_host,
            port=redis_port,
            password=redis_password,
            db=redis_db,
            decode_responses=True,
        )
        self.redis_helper = RedisHelper(self.redis_client)
        self.key_manager = KeyManager(self.redis_client)
        self.validator = TokenValidator(self.redis_client, ip_binding_enabled, watermark_secret)
        self.token_ttl = timedelta(seconds=token_ttl_seconds)
        self.issuer = issuer
        self.ip_binding_enabled = ip_binding_enabled
        self.watermark_secret = watermark_secret

    def generate_token(
        self,
        subject: str,
        extra_claims: Optional[Dict[str, Any]] = None,
        client_ip: Optional[str] = None,
    ) -> str:
        """
        Generate a new JWT using a quantum‑derived key from Redis.
        """
        # 1. Pop a key from entropy pool
        key_bytes = self.redis_helper.pop_key()
        if key_bytes is None:
            raise EntropyExhaustedError("No entropy available in pool")

        # 2. Create key ID and store key in Redis with TTL
        kid = self.key_manager.store_key(key_bytes, self.token_ttl)

        # 3. Build header and claims
        header = {"alg": "HS256", "typ": "JWT", "kid": kid}
        now = int(time.time())
        payload = {
            "sub": subject,
            "iss": self.issuer,
            "iat": now,
            "exp": now + int(self.token_ttl.total_seconds()),
        }
        if extra_claims:
            payload.update(extra_claims)
        if self.ip_binding_enabled and client_ip:
            payload["ip"] = client_ip
        if self.watermark_secret:
            payload["qwm"] = compute_watermark(payload, self.watermark_secret)

        # 4. Sign token
        token = self._sign_token(header, payload, key_bytes)
        return token

    def _sign_token(self, header: dict, payload: dict, key: bytes) -> str:
        """Internal signing method (JWS compact serialization)."""
        header_b64 = base64.urlsafe_b64encode(json.dumps(header, separators=(",", ":")).encode()).decode().rstrip("=")
        payload_b64 = base64.urlsafe_b64encode(json.dumps(payload, separators=(",", ":")).encode()).decode().rstrip("=")
        signing_input = f"{header_b64}.{payload_b64}"
        signature = hmac.new(key, signing_input.encode(), hashlib.sha256).digest()
        signature_b64 = base64.urlsafe_b64encode(signature).decode().rstrip("=")
        return f"{signing_input}.{signature_b64}"

    def validate_token(self, token: str, client_ip: Optional[str] = None) -> dict:
        """
        Validate a JWT. Returns the payload if valid.
        Raises InvalidTokenError or HoneyTokenError on failure.
        """
        return self.validator.validate(token, client_ip)
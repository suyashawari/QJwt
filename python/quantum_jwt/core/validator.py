import json
import base64
import hmac
import hashlib
from typing import Optional
import redis
from ..exceptions import InvalidTokenError, HoneyTokenError
from ..utils.watermark import verify_watermark

class TokenValidator:
    """Validates JWTs and checks security constraints."""

    POISON_KEYS_SET = "poison_keys"

    def __init__(
        self,
        redis_client: redis.Redis,
        ip_binding_enabled: bool,
        watermark_secret: Optional[str],
    ):
        self.redis = redis_client
        self.ip_binding_enabled = ip_binding_enabled
        self.watermark_secret = watermark_secret

    def validate(self, token: str, client_ip: Optional[str] = None) -> dict:
        """
        Validate token, check honey token, IP binding, watermark.
        Returns payload if valid.
        """
        try:
            header_b64, payload_b64, signature_b64 = token.split(".")
        except ValueError:
            raise InvalidTokenError("Malformed token")

        # Decode header to get kid
        header_json = base64.urlsafe_b64decode(header_b64 + "==").decode()
        header = json.loads(header_json)
        kid = header.get("kid")
        if not kid:
            raise InvalidTokenError("Missing kid in header")

        # Check honey token
        if self.redis.sismember(self.POISON_KEYS_SET, kid):
            raise HoneyTokenError("Honey token detected")

        # Retrieve key
        key_b64 = self.redis.get(f"key:{kid}")
        if key_b64 is None:
            raise InvalidTokenError("Key expired or revoked")
        key = base64.urlsafe_b64decode(key_b64 + "==")

        # Verify signature
        signing_input = f"{header_b64}.{payload_b64}"
        expected_sig = hmac.new(key, signing_input.encode(), hashlib.sha256).digest()
        expected_sig_b64 = base64.urlsafe_b64encode(expected_sig).decode().rstrip("=")
        if not hmac.compare_digest(signature_b64, expected_sig_b64):
            raise InvalidTokenError("Invalid signature")

        # Decode payload
        payload_json = base64.urlsafe_b64decode(payload_b64 + "==").decode()
        payload = json.loads(payload_json)

        # Check expiration
        if payload.get("exp", 0) < self._now():
            raise InvalidTokenError("Token expired")

        # IP binding
        if self.ip_binding_enabled:
            token_ip = payload.get("ip")
            if token_ip and token_ip != client_ip:
                raise InvalidTokenError("IP address mismatch")

        # Watermark
        if self.watermark_secret:
            if not verify_watermark(payload, self.watermark_secret):
                raise InvalidTokenError("Invalid watermark")

        return payload

    def _now(self) -> int:
        import time
        return int(time.time())
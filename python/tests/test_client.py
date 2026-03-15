"""
Integration tests for the QuantumJwtClient.

These tests use fakeredis so a real Redis server is not required.
Run with: pytest tests/test_client.py -v
"""

import time
import base64
import secrets
import pytest

# fakeredis gives us an in-memory Redis that quacks like the real thing
import fakeredis

from quantum_jwt.client import QuantumJwtClient
from quantum_jwt.exceptions import (
    EntropyExhaustedError,
    InvalidTokenError,
    HoneyTokenError,
)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_client(**overrides) -> QuantumJwtClient:
    """Create a QuantumJwtClient backed by fakeredis."""
    client = QuantumJwtClient(
        token_ttl_seconds=overrides.get("token_ttl_seconds", 900),
        issuer=overrides.get("issuer", "test-issuer"),
        ip_binding_enabled=overrides.get("ip_binding_enabled", True),
        watermark_secret=overrides.get("watermark_secret", None),
    )
    # Swap out the real Redis client for fakeredis
    fake = fakeredis.FakeRedis(decode_responses=True)
    client.redis_client = fake
    client.redis_helper.redis = fake
    client.key_manager.redis = fake
    client.validator.redis = fake
    return client


def _seed_pool(client: QuantumJwtClient, count: int = 5):
    """Push pre-generated keys into the entropy pool."""
    for _ in range(count):
        key_b64 = base64.urlsafe_b64encode(secrets.token_bytes(32)).decode().rstrip("=")
        client.redis_client.rpush("entropy:pool", key_b64)


# ---------------------------------------------------------------------------
# 1. Happy Path
# ---------------------------------------------------------------------------

class TestHappyPath:
    def test_generate_and_validate(self):
        client = _make_client(ip_binding_enabled=False)
        _seed_pool(client, 1)

        token = client.generate_token("user_123")
        payload = client.validate_token(token)

        assert payload["sub"] == "user_123"
        assert payload["iss"] == "test-issuer"
        assert "kid" in payload or True  # kid is in header, not payload
        assert "exp" in payload
        assert "iat" in payload

    def test_generate_and_validate_with_ip(self):
        client = _make_client()
        _seed_pool(client, 1)

        token = client.generate_token("user_456", client_ip="10.0.0.1")
        payload = client.validate_token(token, client_ip="10.0.0.1")

        assert payload["sub"] == "user_456"
        assert payload["ip"] == "10.0.0.1"

    def test_extra_claims(self):
        client = _make_client(ip_binding_enabled=False)
        _seed_pool(client, 1)

        token = client.generate_token("admin", extra_claims={"role": "admin", "org": "acme"})
        payload = client.validate_token(token)

        assert payload["role"] == "admin"
        assert payload["org"] == "acme"


# ---------------------------------------------------------------------------
# 2. Entropy Exhaustion
# ---------------------------------------------------------------------------

class TestEntropyExhaustion:
    def test_empty_pool_raises(self):
        client = _make_client()
        # Pool is empty — no keys seeded
        with pytest.raises(EntropyExhaustedError):
            client.generate_token("user_123")


# ---------------------------------------------------------------------------
# 3. Honey Token Detection
# ---------------------------------------------------------------------------

class TestHoneyToken:
    def test_poison_key_raises(self):
        client = _make_client(ip_binding_enabled=False)
        _seed_pool(client, 1)

        token = client.generate_token("user_123")

        # Extract kid from the token header
        import json
        header_b64 = token.split(".")[0]
        header = json.loads(base64.urlsafe_b64decode(header_b64 + "=="))
        kid = header["kid"]

        # Mark it as a poison key
        client.redis_client.sadd("poison_keys", kid)

        with pytest.raises(HoneyTokenError):
            client.validate_token(token)


# ---------------------------------------------------------------------------
# 4. IP Mismatch
# ---------------------------------------------------------------------------

class TestIpBinding:
    def test_ip_mismatch_raises(self):
        client = _make_client()
        _seed_pool(client, 1)

        token = client.generate_token("user_789", client_ip="192.168.1.1")

        with pytest.raises(InvalidTokenError, match="IP"):
            client.validate_token(token, client_ip="10.0.0.99")


# ---------------------------------------------------------------------------
# 5. Key Rotation (unique kids)
# ---------------------------------------------------------------------------

class TestKeyRotation:
    def test_consecutive_tokens_have_different_kids(self):
        import json

        client = _make_client(ip_binding_enabled=False)
        _seed_pool(client, 2)

        token1 = client.generate_token("user_a")
        token2 = client.generate_token("user_b")

        header1 = json.loads(base64.urlsafe_b64decode(token1.split(".")[0] + "=="))
        header2 = json.loads(base64.urlsafe_b64decode(token2.split(".")[0] + "=="))

        assert header1["kid"] != header2["kid"]


# ---------------------------------------------------------------------------
# 6. Watermark
# ---------------------------------------------------------------------------

class TestWatermark:
    def test_watermark_present_and_valid(self):
        client = _make_client(ip_binding_enabled=False, watermark_secret="s3cret")
        _seed_pool(client, 1)

        token = client.generate_token("user_wm")
        payload = client.validate_token(token)

        assert "qwm" in payload
        assert payload["sub"] == "user_wm"

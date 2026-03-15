"""
Unit tests for core modules: RiskEngine, KeyManager, and Watermark.

Run with: pytest tests/test_core.py -v
"""

from datetime import timedelta

from quantum_jwt.core.risk_engine import (
    DefaultRiskEvaluator,
    compute_adaptive_ttl,
)
from quantum_jwt.utils.watermark import compute_watermark, verify_watermark
from quantum_jwt.exceptions.errors import security_alert, HoneyTokenError


# ---------------------------------------------------------------------------
# Risk Engine
# ---------------------------------------------------------------------------

class TestDefaultRiskEvaluator:
    def setup_method(self):
        self.evaluator = DefaultRiskEvaluator()

    def test_no_risk(self):
        score = self.evaluator.evaluate({
            "user_agent": "Mozilla/5.0",
            "hour": 12,
            "failed_attempts": 0,
        })
        assert score == 0.0

    def test_empty_user_agent(self):
        score = self.evaluator.evaluate({"user_agent": "", "hour": 12})
        assert score == pytest.approx(0.2)

    def test_off_hours(self):
        score = self.evaluator.evaluate({"user_agent": "Chrome", "hour": 23})
        assert score == pytest.approx(0.3)

    def test_failed_attempts(self):
        score = self.evaluator.evaluate({
            "user_agent": "Chrome",
            "hour": 12,
            "failed_attempts": 3,
        })
        assert score == pytest.approx(0.3)

    def test_max_cap(self):
        score = self.evaluator.evaluate({
            "user_agent": "",
            "hour": 3,
            "failed_attempts": 10,
        })
        assert score == 1.0


class TestAdaptiveTtl:
    def test_no_risk_full_ttl(self):
        ttl = compute_adaptive_ttl(timedelta(minutes=15), 0.0)
        assert ttl == timedelta(minutes=15)

    def test_full_risk_min_ttl(self):
        ttl = compute_adaptive_ttl(timedelta(minutes=15), 1.0)
        assert ttl == timedelta(seconds=60)

    def test_half_risk(self):
        ttl = compute_adaptive_ttl(timedelta(minutes=10), 0.5)
        assert ttl == timedelta(minutes=5)

    def test_custom_min_ttl(self):
        ttl = compute_adaptive_ttl(
            timedelta(minutes=10), 1.0, min_ttl=timedelta(minutes=2)
        )
        assert ttl == timedelta(minutes=2)


# ---------------------------------------------------------------------------
# Watermark
# ---------------------------------------------------------------------------

class TestWatermark:
    def test_compute_and_verify(self):
        payload = {"sub": "user_1", "iat": 1000, "exp": 2000}
        wm = compute_watermark(payload, "secret")
        payload["qwm"] = wm
        assert verify_watermark(payload, "secret") is True

    def test_tampered_payload_fails(self):
        payload = {"sub": "user_1", "iat": 1000, "exp": 2000}
        wm = compute_watermark(payload, "secret")
        payload["qwm"] = wm
        payload["sub"] = "ATTACKER"
        assert verify_watermark(payload, "secret") is False


# ---------------------------------------------------------------------------
# Security Alert Helper
# ---------------------------------------------------------------------------

class TestSecurityAlert:
    def test_honey_token_alert(self):
        err = HoneyTokenError("breach detected")
        alert = security_alert(err, kid="abc-123", ip="10.0.0.1")
        assert alert["level"] == "CRITICAL"
        assert alert["type"] == "HoneyTokenError"
        assert alert["kid"] == "abc-123"


# needed for pytest.approx usage
import pytest

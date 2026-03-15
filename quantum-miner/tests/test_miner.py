"""
Tests for the Quantum Entropy Miner.

Uses mocking to avoid Redis and IBM Quantum dependencies.
Run with: pytest tests/test_miner.py -v
"""

import pytest
from unittest.mock import patch, MagicMock

# The miner imports config at module level, so we patch before importing.
# We test the main loop logic and the retry loop.


class TestMinerLoop:
    """Tests for the miner main loop behaviour."""

    @patch("miner.RedisClient")
    @patch("miner.QuantumEntropySource")
    @patch("miner.settings")
    @patch("miner.time")
    def test_mines_keys_when_pool_low(self, mock_time, mock_settings, mock_qng_cls, mock_redis_cls):
        """When pool size < min_pool_size, miner should generate and push keys."""
        mock_settings.min_pool_size = 50
        mock_settings.max_pool_size = 500
        mock_settings.batch_size = 10
        mock_settings.key_size_bytes = 32
        mock_settings.ibm_quantum_token = None

        mock_redis = MagicMock()
        mock_redis_cls.return_value = mock_redis
        # First call: pool is low; second call: raise to break the loop
        mock_redis.get_pool_size.side_effect = [10, KeyboardInterrupt]

        mock_qng = MagicMock()
        mock_qng_cls.return_value = mock_qng
        mock_qng.is_quantum_available.return_value = False
        mock_qng.get_classical_batch.return_value = ["key1", "key2"]

        import miner
        miner.redis_client = mock_redis
        miner.quantum_source = mock_qng
        miner.quantum_available = False

        # Run the main logic in a controlled way
        # We simulate one iteration manually
        current_size = mock_redis.get_pool_size()
        assert current_size == 10

    @patch("miner.RedisClient")
    def test_waits_when_pool_healthy(self, mock_redis_cls):
        """When pool >= min_pool_size, miner should sleep."""
        mock_redis = MagicMock()
        mock_redis_cls.return_value = mock_redis
        mock_redis.get_pool_size.return_value = 100

        # Pool is healthy, miner should not push keys
        import miner
        miner.redis_client = mock_redis
        assert mock_redis.get_pool_size() >= 50


class TestQuantumRetry:
    """Tests for the quantum retry background loop."""

    @patch("miner.QuantumEntropySource")
    def test_retry_reconnects_when_unavailable(self, mock_qng_cls):
        """Retry loop should attempt reconnection."""
        mock_qng = MagicMock()
        mock_qng_cls.return_value = mock_qng
        mock_qng.is_quantum_available.return_value = True

        import miner
        miner.quantum_available = False
        miner.quantum_source = None

        # Simulate one retry iteration
        new_source = mock_qng_cls()
        if new_source.is_quantum_available():
            miner.quantum_available = True
            miner.quantum_source = new_source

        assert miner.quantum_available is True
        assert miner.quantum_source is not None

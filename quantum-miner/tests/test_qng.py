"""
Tests for the QuantumEntropySource (qng_source.py).

Tests classical fallback and batch generation without IBM Quantum.
Run with: pytest tests/test_qng.py -v
"""

import base64
import pytest
from unittest.mock import patch, MagicMock


class TestClassicalEntropy:
    """Tests for classical (CSPRNG) key generation."""

    def test_classical_batch_returns_correct_count(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = False

            keys = source.get_classical_batch(5, 32)
            assert len(keys) == 5

    def test_classical_keys_are_valid_base64(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = False

            keys = source.get_classical_batch(3, 32)
            for key in keys:
                # Should be URL-safe base64 decodable
                padded = key + "==" 
                decoded = base64.urlsafe_b64decode(padded)
                assert len(decoded) == 32

    def test_classical_keys_are_unique(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = False

            keys = source.get_classical_batch(10, 32)
            assert len(set(keys)) == 10  # all unique


class TestQuantumFallback:
    """Tests for the quantum → classical fallback."""

    def test_fallback_to_classical_when_quantum_unavailable(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = False

            keys = source.get_entropy_batch(5, 32)
            assert len(keys) == 5  # should use classical fallback

    def test_fallback_when_quantum_fails(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = True
            source._generate_quantum_entropy = MagicMock(side_effect=Exception("Quantum error"))

            keys = source.get_entropy_batch(3, 32)
            assert len(keys) == 3  # should fall back to classical
            assert source._quantum_available is False  # should be marked unavailable


class TestQuantumAvailability:
    """Tests for the is_quantum_available check."""

    def test_reports_unavailable_without_qiskit(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = False
            assert source.is_quantum_available() is False

    def test_reports_available_when_initialized(self):
        from qng_source import QuantumEntropySource

        with patch.object(QuantumEntropySource, '_init_quantum'):
            source = QuantumEntropySource()
            source._quantum_available = True
            assert source.is_quantum_available() is True

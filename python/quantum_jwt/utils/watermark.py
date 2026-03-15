import hashlib
import hmac
import json
from typing import Dict, Any

def compute_watermark(payload: Dict[str, Any], secret: str) -> str:
    """
    Compute a quantum watermark for the payload.
    The watermark is an HMAC of the sorted payload (excluding the watermark itself).
    """
    # Copy and remove existing qwm if present (shouldn't be)
    data = payload.copy()
    data.pop("qwm", None)
    # Sort keys for deterministic serialization
    canonical = json.dumps(data, separators=(",", ":"), sort_keys=True)
    return hmac.new(secret.encode(), canonical.encode(), hashlib.sha256).hexdigest()

def verify_watermark(payload: Dict[str, Any], secret: str) -> bool:
    """Verify the watermark in the payload."""
    provided = payload.get("qwm")
    if not provided:
        return False
    expected = compute_watermark(payload, secret)
    return hmac.compare_digest(provided, expected)
"""
Adaptive Risk Engine — Strategy Pattern implementation.

Evaluates request context to compute a risk score that dynamically
adjusts the token TTL.  Higher risk → shorter token lifetime.
"""

from abc import ABC, abstractmethod
from datetime import timedelta
from typing import Dict, Any, Optional


class RiskEvaluator(ABC):
    """
    Abstract base for risk evaluation strategies.
    Implement this to plug in custom risk logic.
    """

    @abstractmethod
    def evaluate(self, context: Dict[str, Any]) -> float:
        """
        Return a risk score between 0.0 (no risk) and 1.0 (maximum risk).

        Parameters
        ----------
        context : dict
            Arbitrary request metadata.  Common keys:
            - ``ip``          : client IP address (str)
            - ``user_agent``  : User-Agent header (str)
            - ``hour``        : hour of request in UTC (int, 0-23)
            - ``failed_attempts`` : recent failed auth count (int)
        """
        ...


class DefaultRiskEvaluator(RiskEvaluator):
    """
    Simple heuristic risk evaluator.

    Scoring rules (cumulative, capped at 1.0):
        +0.2  — missing or empty user-agent
        +0.3  — request outside business hours (22:00–06:00 UTC)
        +0.1  — for every failed auth attempt (up to +0.5)
    """

    def evaluate(self, context: Dict[str, Any]) -> float:
        score = 0.0

        # Suspicious user-agent
        user_agent = context.get("user_agent", "")
        if not user_agent or user_agent.strip() == "":
            score += 0.2

        # Off-hours access
        hour = context.get("hour")
        if hour is not None and (hour >= 22 or hour < 6):
            score += 0.3

        # Failed attempts
        failed = context.get("failed_attempts", 0)
        score += min(failed * 0.1, 0.5)

        return min(score, 1.0)


def compute_adaptive_ttl(
    base_ttl: timedelta,
    risk_score: float,
    min_ttl: Optional[timedelta] = None,
) -> timedelta:
    """
    Shorten the token TTL proportionally to the risk score.

    Parameters
    ----------
    base_ttl : timedelta
        The default (maximum) token lifetime.
    risk_score : float
        A value in [0.0, 1.0] from a ``RiskEvaluator``.
    min_ttl : timedelta, optional
        Floor for the TTL.  Defaults to 60 seconds.

    Returns
    -------
    timedelta
        Adjusted TTL — ``base_ttl * (1 - risk_score)``, clamped to *min_ttl*.
    """
    if min_ttl is None:
        min_ttl = timedelta(seconds=60)

    factor = max(1.0 - risk_score, 0.0)
    adjusted = timedelta(seconds=base_ttl.total_seconds() * factor)
    return max(adjusted, min_ttl)

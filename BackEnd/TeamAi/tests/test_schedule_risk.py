"""Tests unitaires : calcul planning vs avancement (schedule-risk-v1)."""
from __future__ import annotations

from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import pytest

from app.schedule_risk import GAP_AT_RISK, assess_schedule_overrun

BASE = datetime(2025, 1, 1, 12, 0, 0, tzinfo=timezone.utc)


def _iso(dt: datetime) -> str:
    return dt.isoformat().replace("+00:00", "Z")


@patch("app.schedule_risk._utc_now")
def test_ahead_of_schedule_negative_gap_score_zero(mock_now):
    """Peu de temps écoulé, avancement déjà à 33 % → écart négatif, niveau OK, score 0."""
    mock_now.return_value = BASE

    end = BASE + timedelta(days=100)
    project = {
        "createdAt": _iso(BASE),
        "duration": 100,
    }
    phases = [
        {"id": 1, "status": "APPROVED", "startDate": _iso(BASE), "dueDate": _iso(end)},
        {"id": 2, "status": "PLANNED", "startDate": _iso(BASE), "dueDate": _iso(end)},
        {"id": 3, "status": "PLANNED", "startDate": _iso(BASE), "dueDate": _iso(end)},
    ]

    out = assess_schedule_overrun(project, phases, [])

    assert out["scheduleRiskLevel"] == "OK"
    assert out["scheduleRiskScore0To100"] == 0
    m = out["metrics"]
    assert m["timeElapsedRatio"] == pytest.approx(0.0, abs=1e-3)
    assert m["progressRatio"] == pytest.approx(1 / 3, abs=1e-3)
    assert m["gapProgressVsTime"] == pytest.approx(-1 / 3, abs=1e-3)
    assert m["overduePhasesCount"] == 0


@patch("app.schedule_risk._utc_now")
def test_at_risk_when_gap_exceeds_threshold(mock_now):
    end = BASE + timedelta(days=100)
    mock_now.return_value = BASE + timedelta(days=50)

    project = {"createdAt": _iso(BASE), "duration": 100}
    phases = [
        {"id": 1, "status": "PLANNED", "startDate": _iso(BASE), "dueDate": _iso(end)},
    ]

    out = assess_schedule_overrun(project, phases, [])

    assert out["metrics"]["timeElapsedRatio"] == pytest.approx(0.5, abs=1e-2)
    assert out["metrics"]["progressRatio"] == 0.0
    assert out["metrics"]["gapProgressVsTime"] >= GAP_AT_RISK - 1e-6
    assert out["scheduleRiskLevel"] == "AT_RISK"
    assert out["scheduleRiskScore0To100"] > 0


@patch("app.schedule_risk._utc_now")
def test_overdue_phase_not_approved_counts_and_at_risk(mock_now):
    due = BASE + timedelta(days=10)
    mock_now.return_value = BASE + timedelta(days=40)

    project = {"createdAt": _iso(BASE), "duration": 90}
    phases = [
        {
            "id": 1,
            "status": "IN_PROGRESS",
            "startDate": _iso(BASE),
            "dueDate": _iso(due),
        },
    ]
    out = assess_schedule_overrun(project, phases, [])

    assert out["metrics"]["overduePhasesCount"] == 1
    assert out["scheduleRiskLevel"] == "AT_RISK"
    assert "PHASES_EN_RETARD:1" in out["flags"]


def test_incomplete_planning_returns_flag():
    out = assess_schedule_overrun({}, [], [])

    assert out["scheduleRiskLevel"] == "OK"
    assert "PLANNING_INCOMPLET" in out["flags"]
    assert out["metrics"].get("timeElapsedRatio") is None

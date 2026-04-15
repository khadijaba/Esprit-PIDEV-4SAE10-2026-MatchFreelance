"""
Prévision de dépassement planning : temps écoulé vs avancement (phases / livrables).

Heuristique « schedule-risk-v1 » :
- ratio temps écoulé sur la fenêtre projet [début → fin prévue] ;
- ratio d'avancement = livrables ACCEPTED / total (sinon phases APPROVED / total) ;
- écart (temps - avancement) + phases en retard (dueDate passée et non APPROVED).
"""
from __future__ import annotations

import os
from datetime import datetime, timedelta, timezone
from typing import Any

import httpx

GATEWAY_URL = os.environ.get("GATEWAY_URL", "http://127.0.0.1:8086").rstrip("/")
METHOD_VERSION = "schedule-risk-v1"

# Seuils (réglables via env)
GAP_WATCH = float(os.environ.get("SCHEDULE_GAP_WATCH", "0.10"))
GAP_AT_RISK = float(os.environ.get("SCHEDULE_GAP_AT_RISK", "0.18"))
MIN_TIME_FOR_ALERT = float(os.environ.get("SCHEDULE_MIN_TIME_ALERT", "0.22"))


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _parse_dt(v: Any) -> datetime | None:
    if v is None:
        return None
    if isinstance(v, (int, float)):
        return None
    s = str(v).strip()
    if not s:
        return None
    try:
        if s.endswith("Z"):
            s = s[:-1] + "+00:00"
        dt = datetime.fromisoformat(s)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except ValueError:
        return None


def _phase_id(p: dict[str, Any]) -> int | None:
    x = p.get("id")
    if x is None:
        return None
    try:
        return int(x)
    except (TypeError, ValueError):
        return None


def _project_window(
    project: dict[str, Any], phases: list[dict[str, Any]]
) -> tuple[datetime | None, datetime | None]:
    """Début / fin prévus pour positionner le projet sur l'axe temps."""
    created = _parse_dt(project.get("createdAt"))
    duration_days = project.get("duration")
    try:
        ddays = int(duration_days) if duration_days is not None else None
    except (TypeError, ValueError):
        ddays = None

    starts: list[datetime] = []
    dues: list[datetime] = []
    for p in phases:
        sd = _parse_dt(p.get("startDate"))
        dd = _parse_dt(p.get("dueDate"))
        if sd:
            starts.append(sd)
        if dd:
            dues.append(dd)

    start = min(starts) if starts else created
    end = None
    if dues:
        end = max(dues)
    if end is None and start is not None and ddays is not None and ddays > 0:
        end = start + timedelta(days=ddays)
    if end is None and created is not None and ddays is not None and ddays > 0:
        end = created + timedelta(days=ddays)
    return start, end


def assess_schedule_overrun(
    project: dict[str, Any],
    phases: list[dict[str, Any]],
    deliverables: list[dict[str, Any]],
) -> dict[str, Any]:
    now = _utc_now()
    start, end = _project_window(project, phases)
    flags: list[str] = []

    if start is None or end is None:
        return {
            "scheduleRiskLevel": "OK",
            "scheduleRiskScore0To100": 0,
            "summary": "Planning insuffisant : renseignez des phases avec dates ou une durée projet pour estimer le risque de retard.",
            "flags": ["PLANNING_INCOMPLET"],
            "metrics": {
                "timeElapsedRatio": None,
                "progressRatio": None,
                "gapProgressVsTime": None,
                "deliverablesAccepted": 0,
                "deliverablesTotal": len(deliverables),
                "overduePhasesCount": 0,
                "methodVersion": METHOD_VERSION,
            },
            "banner": None,
        }

    span_s = (end - start).total_seconds()
    if span_s <= 0:
        flags.append("FENETRE_TEMPS_INVALIDE")
        return {
            "scheduleRiskLevel": "OK",
            "scheduleRiskScore0To100": 0,
            "summary": "Fenêtre temporelle projet invalide (fin avant début).",
            "flags": flags,
            "metrics": {
                "timeElapsedRatio": None,
                "progressRatio": None,
                "gapProgressVsTime": None,
                "deliverablesAccepted": 0,
                "deliverablesTotal": len(deliverables),
                "overduePhasesCount": 0,
                "methodVersion": METHOD_VERSION,
            },
            "banner": None,
        }

    elapsed_s = max(0.0, (now - start).total_seconds())
    time_elapsed_ratio = min(1.0, max(0.0, elapsed_s / span_s))

    accepted = 0
    total_d = len(deliverables)
    for d in deliverables:
        st = str(d.get("reviewStatus") or "").upper()
        if st == "ACCEPTED":
            accepted += 1

    if total_d > 0:
        progress_ratio = accepted / total_d
    else:
        approved_ph = sum(1 for p in phases if str(p.get("status") or "").upper() == "APPROVED")
        total_ph = len(phases)
        progress_ratio = (approved_ph / total_ph) if total_ph > 0 else 0.0
        if total_ph == 0:
            flags.append("AUCUNE_PHASE")

    gap = time_elapsed_ratio - progress_ratio

    overdue = 0
    for p in phases:
        st = str(p.get("status") or "").upper()
        due = _parse_dt(p.get("dueDate"))
        if due and due < now and st != "APPROVED":
            overdue += 1
    if overdue:
        flags.append(f"PHASES_EN_RETARD:{overdue}")

    level = "OK"
    score = int(round(max(0.0, min(100.0, gap * 100 + overdue * 15))))

    if overdue >= 1 or gap >= GAP_AT_RISK:
        level = "AT_RISK"
    elif gap >= GAP_WATCH and time_elapsed_ratio >= MIN_TIME_FOR_ALERT:
        level = "WATCH"
    elif gap >= GAP_WATCH:
        level = "WATCH"

    if level == "OK" and gap < GAP_WATCH and overdue == 0:
        score = max(0, min(25, score))

    summary_parts = [
        f"Temps écoulé ~ {time_elapsed_ratio * 100:.0f} % de la fenêtre planifiée.",
        f"Avancement ~ {progress_ratio * 100:.0f} % (livrables validés ou phases approuvées).",
    ]
    if gap > 0.05:
        summary_parts.append(
            f"Écart (temps - avancement) ~ {gap * 100:.0f} points — risque de retard si la tendance continue."
        )
    if overdue:
        summary_parts.append(f"{overdue} phase(s) avec échéance dépassée et non clôturée(s).")

    summary = " ".join(summary_parts)

    banner = None
    if level == "WATCH":
        banner = {
            "title": "Planning à surveiller",
            "message": summary,
            "severity": "warning",
        }
    elif level == "AT_RISK":
        banner = {
            "title": "Risque de dépassement",
            "message": summary,
            "severity": "danger",
        }

    return {
        "scheduleRiskLevel": level,
        "scheduleRiskScore0To100": score,
        "summary": summary,
        "flags": flags,
        "metrics": {
            "timeElapsedRatio": round(time_elapsed_ratio, 4),
            "progressRatio": round(progress_ratio, 4),
            "gapProgressVsTime": round(gap, 4),
            "deliverablesAccepted": accepted,
            "deliverablesTotal": total_d,
            "overduePhasesCount": overdue,
            "methodVersion": METHOD_VERSION,
        },
        "banner": banner,
    }


async def fetch_supervision_data(
    project_id: int,
    authorization: str | None,
) -> tuple[dict[str, Any], list[dict[str, Any]], list[dict[str, Any]]]:
    headers: dict[str, str] = {}
    if authorization:
        headers["Authorization"] = authorization

    async with httpx.AsyncClient(timeout=45.0) as client:
        pr = await client.get(f"{GATEWAY_URL}/api/projects/{project_id}", headers=headers)
        pr.raise_for_status()
        project = pr.json()

        ph_r = await client.get(
            f"{GATEWAY_URL}/api/projects/{project_id}/phases",
            headers=headers,
        )
        ph_r.raise_for_status()
        phases_raw = ph_r.json()
        phases: list[dict[str, Any]] = phases_raw if isinstance(phases_raw, list) else []

        all_deliverables: list[dict[str, Any]] = []
        for p in phases:
            pid = _phase_id(p)
            if pid is None:
                continue
            d_r = await client.get(
                f"{GATEWAY_URL}/api/projects/{project_id}/phases/{pid}/deliverables",
                headers=headers,
            )
            if d_r.status_code != 200:
                continue
            arr = d_r.json()
            if isinstance(arr, list):
                all_deliverables.extend(arr)

    return project, phases, all_deliverables


async def run_assessment(
    project_id: int,
    authorization: str | None,
    project_snapshot: dict[str, Any] | None,
    phases_snapshot: list[dict[str, Any]] | None,
    deliverables_snapshot: list[dict[str, Any]] | None,
) -> dict[str, Any]:
    if (
        project_snapshot is not None
        and phases_snapshot is not None
        and deliverables_snapshot is not None
    ):
        return assess_schedule_overrun(project_snapshot, phases_snapshot, deliverables_snapshot)

    try:
        project, phases, dels = await fetch_supervision_data(project_id, authorization)
    except httpx.HTTPStatusError as e:
        return {
            "scheduleRiskLevel": "OK",
            "scheduleRiskScore0To100": 0,
            "summary": f"Impossible de charger le projet ou la supervision (HTTP {e.response.status_code}).",
            "flags": ["GATEWAY_OR_AUTH_ERROR"],
            "metrics": {"methodVersion": METHOD_VERSION},
            "banner": None,
            "error": str(e),
        }
    except Exception as e:
        return {
            "scheduleRiskLevel": "OK",
            "scheduleRiskScore0To100": 0,
            "summary": "Erreur lors de l'analyse de planning (gateway ou réseau).",
            "flags": ["FETCH_ERROR"],
            "metrics": {"methodVersion": METHOD_VERSION},
            "banner": None,
            "error": str(e),
        }
    return assess_schedule_overrun(project, phases, dels)

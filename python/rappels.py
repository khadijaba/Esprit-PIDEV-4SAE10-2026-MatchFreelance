"""
Service de rappels : interroge Formation, Evaluation, User ;
détecte formations ouvertes, examens à passer, nouveaux certificats,
formation se termine dans X jours, examen non passé après X jours ;
envoie emails (SMTP) ou webhook (Slack/Teams).
À lancer en cron (ex. toutes les heures) ou à la main.
"""
import smtplib
import sys
from datetime import datetime, timedelta
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import Any

import requests

from config import (
    API_BASE_URL,
    API_TOKEN,
    APP_BASE_URL,
    RAPPEL_EXAMEN_JOURS_RETARD,
    RAPPEL_FORMATION_JOURS_AVANT_FIN,
    RAPPELS_ALWAYS_SEND_TO,
    SEND_EMAIL_WHEN_NO_RAPPELS,
    SMTP_FROM,
    SMTP_HOST,
    SMTP_PASSWORD,
    SMTP_PORT,
    SMTP_USER,
    WEBHOOK_SLACK,
    WEBHOOK_URL,
)


def api_get(path: str) -> list[Any] | dict[str, Any] | None:
    """GET sur la Gateway (api). Retourne liste ou dict selon l'endpoint."""
    url = f"{API_BASE_URL}{path}"
    headers = {}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    try:
        r = requests.get(url, headers=headers, timeout=10)
        r.raise_for_status()
        return r.json()
    except requests.RequestException as e:
        print(f"[Rappels] Erreur API {path}: {e}", file=sys.stderr)
        return None


def fetch_freelancers() -> list[dict]:
    """Liste des utilisateurs avec rôle FREELANCER (User)."""
    data = api_get("/api/users")
    if not isinstance(data, list):
        return []
    return [u for u in data if u.get("role") == "FREELANCER" and u.get("email")]


def fetch_formations_ouvertes() -> list[dict]:
    """Formations avec statut OUVERTE (Formation)."""
    data = api_get("/api/formations/ouvertes")
    return data if isinstance(data, list) else []


def fetch_inscriptions_freelancer(freelancer_id: int) -> list[dict]:
    """Inscriptions d'un freelancer (Formation)."""
    data = api_get(f"/api/inscriptions/freelancer/{freelancer_id}")
    return data if isinstance(data, list) else []


def fetch_examens_formation(formation_id: int) -> list[dict]:
    """Examens liés à une formation (Evaluation)."""
    data = api_get(f"/api/examens/formation/{formation_id}")
    return data if isinstance(data, list) else []


def fetch_resultats_freelancer(freelancer_id: int) -> list[dict]:
    """Résultats d'examens passés par un freelancer (Evaluation)."""
    data = api_get(f"/api/examens/resultats/freelancer/{freelancer_id}")
    return data if isinstance(data, list) else []


def fetch_certificats_freelancer(freelancer_id: int) -> list[dict]:
    """Certificats obtenus par un freelancer (Evaluation)."""
    data = api_get(f"/api/certificats/freelancer/{freelancer_id}")
    return data if isinstance(data, list) else []


def build_rappels_for_freelancer(
    freelancer: dict,
    formations_ouvertes: list[dict],
    inscriptions: list[dict],
    resultats: list[dict],
    certificats: list[dict],
) -> list[dict]:
    """
    Construit la liste des rappels pour un freelancer.
    Chaque rappel : { "type": "...", "message": "...", "detail": "..." }
    """
    rappels = []
    fid = freelancer.get("id") or freelancer.get("userId")
    if not fid:
        return rappels

    # 1) Formations ouvertes (une ligne globale ou par formation)
    if formations_ouvertes:
        titres = [f.get("titre", f"Formation #{f.get('id')}") for f in formations_ouvertes[:5]]
        rappels.append({
            "type": "formation_ouverte",
            "message": "Vous avez des formations ouvertes disponibles.",
            "detail": "; ".join(titres),
        })

    # 2) Examens à passer : inscriptions validées dont l'examen n'a pas été passé (examen bien mis en avant dans l'email)
    inscriptions_validees = [i for i in inscriptions if i.get("statut") == "VALIDEE"]
    examens_deja_passes = {r.get("examenId") for r in resultats if r.get("examenId")}
    for ins in inscriptions_validees:
        formation_id = ins.get("formationId")
        formation_titre = ins.get("formationTitre") or f"Formation #{formation_id}"
        examens = fetch_examens_formation(formation_id) if formation_id else []
        for ex in examens:
            if ex.get("id") not in examens_deja_passes:
                examen_titre = ex.get("titre") or f"Examen #{ex.get('id')}"
                rappels.append({
                    "type": "examen_a_passer",
                    "message": f"Examen à passer pour la formation : {formation_titre}",
                    "detail": f"Examen : {examen_titre}",
                    "examenTitre": examen_titre,
                    "formationId": formation_id,
                    "examenId": ex.get("id"),
                })

    # 2b) Rappel « formation se termine dans X jours »
    try:
        today = datetime.now().date()
        delta_fin = timedelta(days=RAPPEL_FORMATION_JOURS_AVANT_FIN)
    except Exception:
        delta_fin = timedelta(days=7)
    formation_ids_inscrit = {i.get("formationId") for i in inscriptions if i.get("formationId")}
    for f in formations_ouvertes:
        formation_id = f.get("id")
        if formation_id not in formation_ids_inscrit:
            continue
        date_fin = f.get("dateFin")
        if not date_fin:
            continue
        try:
            fin = datetime.strptime(date_fin[:10], "%Y-%m-%d").date()
            if 0 <= (fin - today).days <= RAPPEL_FORMATION_JOURS_AVANT_FIN:
                rappels.append({
                    "type": "formation_se_termine",
                    "message": f"La formation « {f.get('titre', '') or ('#' + str(formation_id))} » se termine bientôt.",
                    "detail": f"Fin prévue le {date_fin[:10]} — pensez à passer l'examen si ce n'est pas fait.",
                })
        except (ValueError, TypeError):
            pass

    # 2c) Rappel « examen non passé après X jours » (inscription validée depuis longtemps)
    try:
        today = datetime.now().date()
        seuil_jours = RAPPEL_EXAMEN_JOURS_RETARD
    except Exception:
        seuil_jours = 14
    for ins in inscriptions_validees:
        formation_id = ins.get("formationId")
        formation_titre = ins.get("formationTitre") or f"Formation #{formation_id}"
        date_ins = ins.get("dateInscription")
        if not date_ins:
            continue
        try:
            # dateInscription peut être "2025-02-01T10:00:00" ou "2025-02-01"
            di = datetime.fromisoformat(date_ins.replace("Z", "+00:00")).date() if "T" in str(date_ins) else datetime.strptime(str(date_ins)[:10], "%Y-%m-%d").date()
            if (today - di).days < seuil_jours:
                continue
        except (ValueError, TypeError):
            continue
        examens = fetch_examens_formation(formation_id) if formation_id else []
        for ex in examens:
            if ex.get("id") not in examens_deja_passes:
                examen_titre = ex.get("titre") or f"Examen #{ex.get('id')}"
                rappels.append({
                    "type": "examen_non_passe_retard",
                    "message": f"Examen non passé depuis plus de {seuil_jours} jours — formation : {formation_titre}",
                    "detail": f"Examen : {examen_titre}. Passez-le pour valider votre parcours.",
                    "examenTitre": examen_titre,
                    "formationId": formation_id,
                    "examenId": ex.get("id"),
                })
                break  # un rappel par formation en retard

    # 3) Nouveau certificat / certificats obtenus
    if certificats:
        rappels.append({
            "type": "certificat",
            "message": "Nouveau(x) certificat(s) disponible(s)." if len(certificats) > 1 else "Nouveau certificat disponible.",
            "detail": ", ".join(c.get("examenTitre", f"Certificat #{c.get('id')}") for c in certificats[:5]),
        })

    return rappels


def build_email_no_rappels(email_or_name: str) -> tuple[str, str]:
    """Construit un court email « aucun rappel » (pour envoi systématique à certaines adresses)."""
    prenom = (email_or_name or "Freelancer").split("@")[0].split()[0] or "Freelancer"
    body_text = f"""Bonjour {prenom},

Vous n'avez pas de nouveau rappel pour le moment sur MatchFreelance.

Accédez à la plateforme :
  • Formations : {APP_BASE_URL}/formations
  • Mon activité : {APP_BASE_URL}/mon-activite
  • Mon dashboard : {APP_BASE_URL}/dashboard-freelancer

À bientôt sur MatchFreelance !"""
    body_html = f"""
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:Segoe UI,Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#1e293b;line-height:1.5">
  <div style="margin-bottom:24px">
    <span style="font-size:1.25em;font-weight:700;color:#0d9488">Match</span><span style="font-size:1.25em;font-weight:700;color:#ea580c">Freelance</span>
  </div>
  <p style="font-size:1.05em">Bonjour <strong>{prenom}</strong>,</p>
  <p>Vous n'avez pas de nouveau rappel pour le moment.</p>
  <p style="margin-top:24px">
    <a href="{APP_BASE_URL}/formations" style="color:#0d9488;font-weight:600">Voir les formations</a> &nbsp;|&nbsp;
    <a href="{APP_BASE_URL}/dashboard-freelancer" style="color:#0d9488;font-weight:600">Mon dashboard</a>
  </p>
  <p style="margin-top:32px;font-size:0.9em;color:#64748b">À bientôt sur MatchFreelance</p>
</body>
</html>"""
    return body_text, body_html.strip()


def build_email_content(freelancer: dict, rappels: list[dict]) -> tuple[str, str]:
    """Construit le corps de l'email en texte brut et en HTML."""
    prenom = (freelancer.get("fullName") or freelancer.get("email") or "Freelancer").split()[0]
    nb = len(rappels)

    # Types pour titres et icônes (examen toujours bien indiqué dans l'email)
    type_labels = {
        "formation_ouverte": ("Formations ouvertes", "📚"),
        "examen_a_passer": ("Examen à passer", "📝"),
        "examen_non_passe_retard": ("Examen en retard", "⏰"),
        "formation_se_termine": ("Formation se termine", "📅"),
        "certificat": ("Certificats", "🏆"),
    }

    # --- Texte brut ---
    lines = [
        f"Bonjour {prenom},",
        "",
        f"Vous avez {nb} rappel(s) sur votre espace MatchFreelance :",
        "",
    ]
    for r in rappels:
        label, icon = type_labels.get(r["type"], (r["type"], "•"))
        lines.append(f"  {icon} {r['message']}")
        if r.get("detail"):
            lines.append(f"    → {r['detail']}")
        if r.get("type") in ("examen_a_passer", "examen_non_passe_retard") and r.get("formationId") and r.get("examenId"):
            url = f"{APP_BASE_URL}/formations/{r['formationId']}/examen/{r['examenId']}"
            lines.append(f"    Lien direct (passer l'examen) : {url}")
        lines.append("")
    lines.extend([
        "Accédez à la plateforme pour plus de détails :",
        f"  • Formations : {APP_BASE_URL}/formations",
        f"  • Mon activité : {APP_BASE_URL}/mon-activite",
        f"  • Mon dashboard : {APP_BASE_URL}/dashboard-freelancer",
        "",
        "À bientôt sur MatchFreelance !",
    ])
    body_text = "\n".join(lines)

    # --- HTML ---
    rappels_html = []
    for r in rappels:
        label, icon = type_labels.get(r["type"], (r["type"], "•"))
        detail = f"<p style='margin:0 0 0 1.5em;color:#475569;font-size:0.95em'>{r.get('detail', '')}</p>" if r.get("detail") else ""
        lien_examen = ""
        if r.get("type") in ("examen_a_passer", "examen_non_passe_retard") and r.get("formationId") and r.get("examenId"):
            url = f"{APP_BASE_URL}/formations/{r['formationId']}/examen/{r['examenId']}"
            examen_t = r.get("examenTitre") or r.get("detail") or "examen"
            lien_examen = f"<p style='margin:0.25em 0 0 1.5em'><a href='{url}' style='color:#0d9488;font-weight:600'>Passer l'examen : {examen_t} →</a></p>"
        rappels_html.append(f"""
        <div style='margin-bottom:1em;padding:0.75em;background:#f8fafc;border-radius:8px;border-left:4px solid #0d9488'>
          <strong>{icon} {r['message']}</strong>
          {detail}
          {lien_examen}
        </div>""")
    rappels_block = "\n".join(rappels_html)

    body_html = f"""
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
<body style="font-family:Segoe UI,Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;color:#1e293b;line-height:1.5">
  <div style="margin-bottom:24px">
    <span style="font-size:1.25em;font-weight:700;color:#0d9488">Match</span><span style="font-size:1.25em;font-weight:700;color:#ea580c">Freelance</span>
  </div>
  <p style="font-size:1.05em">Bonjour <strong>{prenom}</strong>,</p>
  <p>Vous avez <strong>{nb} rappel(s)</strong> sur votre espace :</p>
  {rappels_block}
  <p style="margin-top:24px">
    <a href="{APP_BASE_URL}/formations" style="color:#0d9488;text-decoration:none;font-weight:600">→ Voir les formations</a> &nbsp;|&nbsp;
    <a href="{APP_BASE_URL}/mon-activite" style="color:#0d9488;text-decoration:none;font-weight:600">Mon activité</a> &nbsp;|&nbsp;
    <a href="{APP_BASE_URL}/dashboard-freelancer" style="color:#0d9488;text-decoration:none;font-weight:600">Mon dashboard</a>
  </p>
  <p style="margin-top:32px;font-size:0.9em;color:#64748b">À bientôt sur MatchFreelance — Connecting Skills to Opportunities</p>
</body>
</html>"""
    return body_text, body_html.strip()


def send_email(to: str, subject: str, body_text: str, body_html: str = "") -> bool:
    """Envoi d'un email via SMTP (texte + HTML si fourni)."""
    if not SMTP_HOST or not SMTP_USER:
        print("[Rappels] SMTP non configuré (SMTP_HOST, SMTP_USER). Email non envoyé.", file=sys.stderr)
        return False
    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = SMTP_FROM
    msg["To"] = to
    msg.attach(MIMEText(body_text, "plain", "utf-8"))
    if body_html:
        msg.attach(MIMEText(body_html, "html", "utf-8"))
    try:
        with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as s:
            s.starttls()
            if SMTP_USER and SMTP_PASSWORD:
                s.login(SMTP_USER, SMTP_PASSWORD)
            s.sendmail(SMTP_FROM, [to], msg.as_string())
        return True
    except Exception as e:
        print(f"[Rappels] Erreur envoi email à {to}: {e}", file=sys.stderr)
        return False


def format_slack_message(freelancer: dict, rappels: list[dict]) -> str:
    """Format des rappels pour Slack / Teams (texte lisible)."""
    prenom = (freelancer.get("fullName") or freelancer.get("email") or "Freelancer").split()[0]
    lines = [f"*MatchFreelance — Rappels pour {prenom}*"]
    for r in rappels:
        lines.append(f"• {r.get('message', '')}")
        if r.get("detail"):
            lines.append(f"  _{r['detail']}_")
        if r.get("formationId") and r.get("examenId"):
            url = f"{APP_BASE_URL}/formations/{r['formationId']}/examen/{r['examenId']}"
            lines.append(f"  <{url}|Passer l'examen>")
    return "\n".join(lines)


def send_webhook(payload: dict) -> bool:
    """Envoi des rappels vers une URL webhook (POST JSON). Slack/Teams si WEBHOOK_SLACK=true."""
    if not WEBHOOK_URL:
        return False
    body = payload
    if WEBHOOK_SLACK and "rappels" in payload:
        # Un seul payload global avec liste de rappels
        rappels_list = payload.get("rappels", [])
        if rappels_list:
            parts = []
            for item in rappels_list:
                email = item.get("email", "")
                rappels = item.get("rappels", [])
                if rappels:
                    fake_f = {"fullName": "", "email": email}
                    parts.append(format_slack_message(fake_f, rappels))
            body = {"text": "\n\n".join(parts)} if parts else payload
        elif "freelancer" in payload and payload.get("rappels"):
            body = {"text": format_slack_message(payload["freelancer"], payload["rappels"])}
    elif WEBHOOK_SLACK and "freelancer" in payload and "rappels" in payload:
        body = {"text": format_slack_message(payload["freelancer"], payload["rappels"])}
    try:
        r = requests.post(WEBHOOK_URL, json=body, timeout=10)
        r.raise_for_status()
        return True
    except requests.RequestException as e:
        print(f"[Rappels] Erreur webhook: {e}", file=sys.stderr)
        return False


def run_rappels(dry_run: bool = False) -> dict:
    """
    Lance la détection des rappels et envoie par email (ou webhook).
    dry_run=True : affiche les rappels sans envoyer.
    Retourne un résumé { "freelancers": N, "emails_sent": N, "rappels_total": N }.
    """
    freelancers = fetch_freelancers()
    formations_ouvertes = fetch_formations_ouvertes()
    if not freelancers:
        print("[Rappels] Aucun freelancer trouvé (vérifiez User / API).")
        return {"freelancers": 0, "emails_sent": 0, "rappels_total": 0}

    print(f"[Rappels] {len(freelancers)} freelancer(s) — envoi des rappels à TOUS : {[f.get('email') for f in freelancers]}")

    all_rappels_payload = []
    emails_sent = 0
    rappels_total = 0

    sent_to_emails: set[str] = set()

    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        email = f.get("email")
        if not email or not fid:
            continue
        inscriptions = fetch_inscriptions_freelancer(fid)
        resultats = fetch_resultats_freelancer(fid)
        certificats = fetch_certificats_freelancer(fid)
        rappels = build_rappels_for_freelancer(
            f, formations_ouvertes, inscriptions, resultats, certificats
        )
        send_anyway = (SEND_EMAIL_WHEN_NO_RAPPELS or email in RAPPELS_ALWAYS_SEND_TO) if RAPPELS_ALWAYS_SEND_TO or SEND_EMAIL_WHEN_NO_RAPPELS else False
        if not rappels and not send_anyway:
            continue
        rappels_total += len(rappels)
        if rappels:
            body_text, body_html = build_email_content(f, rappels)
            subject = f"MatchFreelance — Vous avez {len(rappels)} rappel(s)"
        else:
            body_text, body_html = build_email_no_rappels(f.get("email") or f.get("fullName") or "")
            subject = "MatchFreelance — Rappels"
        payload_item = {"email": email, "freelancer_id": fid, "rappels": rappels}
        all_rappels_payload.append(payload_item)

        if dry_run:
            print(f"[Rappels] {email}: {len(rappels)} rappel(s)\n{body_text}\n")
            sent_to_emails.add(email)
            continue
        if SMTP_HOST and SMTP_USER:
            if send_email(email, subject, body_text, body_html):
                emails_sent += 1
                sent_to_emails.add(email)
        elif WEBHOOK_URL:
            send_webhook({"freelancer": f, "rappels": rappels})
            emails_sent += 1
            sent_to_emails.add(email)

    # Envoi aux adresses RAPPELS_ALWAYS_SEND_TO qui n'ont pas encore reçu d'email (ex: non inscrites en FREELANCER)
    for to_email in RAPPELS_ALWAYS_SEND_TO:
        if to_email in sent_to_emails or dry_run:
            continue
        if SMTP_HOST and SMTP_USER:
            body_text, body_html = build_email_no_rappels(to_email)
            if send_email(to_email, "MatchFreelance — Rappels", body_text, body_html):
                emails_sent += 1
                print(f"[Rappels] Email « aucun rappel » envoyé à {to_email} (RAPPELS_ALWAYS_SEND_TO)", file=sys.stderr)

    if WEBHOOK_URL and all_rappels_payload and not dry_run and not (SMTP_HOST and SMTP_USER):
        send_webhook({"rappels": all_rappels_payload})
        emails_sent = len([p for p in all_rappels_payload if p["rappels"]])

    return {
        "freelancers": len(freelancers),
        "emails_sent": emails_sent,
        "rappels_total": rappels_total,
    }


if __name__ == "__main__":
    dry = "--dry-run" in sys.argv
    print(f"[Rappels] SMTP utilisé: {SMTP_HOST or '(aucun)'} — Envoi depuis: {SMTP_USER or '(non configuré)'}")
    if SMTP_HOST and "outlook" in SMTP_HOST.lower() and SMTP_USER and "gmail" in SMTP_USER.lower():
        print("[Rappels] ATTENTION: SMTP_HOST est Outlook mais SMTP_USER est Gmail. Pour Gmail, mets SMTP_HOST=smtp.gmail.com dans .env", file=sys.stderr)
    summary = run_rappels(dry_run=dry)
    print(f"[Rappels] Terminé: {summary}")

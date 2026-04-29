"""
Pipeline ETL : agrégation des données User, Skill, Formation, Evaluation
dans une base SQLite (ou PostgreSQL) pour tableaux de bord et rapports.
À lancer en cron (ex. quotidien) ou à la main.
"""
import sqlite3
import sys
from pathlib import Path
from typing import Any

import requests

from config import API_BASE_URL, API_TOKEN, ETL_DB_PATH


def api_get(path: str) -> list[Any] | dict[str, Any] | None:
    """GET sur la Gateway."""
    url = f"{API_BASE_URL}{path}"
    headers = {}
    if API_TOKEN:
        headers["Authorization"] = f"Bearer {API_TOKEN}"
    try:
        r = requests.get(url, headers=headers, timeout=15)
        r.raise_for_status()
        return r.json()
    except requests.RequestException as e:
        print(f"[ETL] Erreur API {path}: {e}", file=sys.stderr)
        return None


def ensure_dir(db_path: str) -> None:
    Path(db_path).parent.mkdir(parents=True, exist_ok=True)


def create_schema(conn: sqlite3.Connection) -> None:
    """Crée les tables analytiques."""
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY,
            email TEXT,
            full_name TEXT,
            role TEXT,
            created_at TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS formations (
            id INTEGER PRIMARY KEY,
            titre TEXT,
            type_formation TEXT,
            description TEXT,
            duree_heures INTEGER,
            date_debut TEXT,
            date_fin TEXT,
            statut TEXT,
            capacite_max INTEGER,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS inscriptions (
            id INTEGER PRIMARY KEY,
            freelancer_id INTEGER,
            formation_id INTEGER,
            formation_titre TEXT,
            statut TEXT,
            date_inscription TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS examens (
            id INTEGER PRIMARY KEY,
            formation_id INTEGER,
            titre TEXT,
            description TEXT,
            seuil_reussi INTEGER,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS passages (
            id INTEGER PRIMARY KEY,
            freelancer_id INTEGER,
            examen_id INTEGER,
            examen_titre TEXT,
            score REAL,
            resultat TEXT,
            date_passage TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS certificats (
            id INTEGER PRIMARY KEY,
            passage_examen_id INTEGER,
            freelancer_id INTEGER,
            examen_id INTEGER,
            examen_titre TEXT,
            numero_certificat TEXT,
            score REAL,
            date_passage TEXT,
            date_delivrance TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS skills (
            id INTEGER PRIMARY KEY,
            name TEXT,
            category TEXT,
            freelancer_id INTEGER,
            level TEXT,
            years_of_experience INTEGER,
            created_at TEXT,
            updated_at TEXT DEFAULT CURRENT_TIMESTAMP
        );
    """)
    conn.commit()


def load_users(conn: sqlite3.Connection) -> int:
    """Charge les utilisateurs (User)."""
    data = api_get("/api/users")
    if not isinstance(data, list):
        return 0
    conn.execute("DELETE FROM users")
    for u in data:
        uid = u.get("id") or u.get("userId")
        if uid is None:
            continue
        conn.execute(
            "INSERT OR REPLACE INTO users (id, email, full_name, role, created_at) VALUES (?, ?, ?, ?, ?)",
            (
                uid,
                u.get("email") or "",
                u.get("fullName") or "",
                u.get("role") or "",
                u.get("createdAt") or "",
            ),
        )
    conn.commit()
    return len(data)


def load_formations(conn: sqlite3.Connection) -> int:
    """Charge les formations (Formation)."""
    data = api_get("/api/formations")
    if not isinstance(data, list):
        return 0
    conn.execute("DELETE FROM formations")
    for f in data:
        fid = f.get("id")
        if fid is None:
            continue
        conn.execute(
            """INSERT OR REPLACE INTO formations
               (id, titre, type_formation, description, duree_heures, date_debut, date_fin, statut, capacite_max)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                fid,
                f.get("titre") or "",
                f.get("typeFormation") or "",
                f.get("description") or "",
                f.get("dureeHeures"),
                f.get("dateDebut") or "",
                f.get("dateFin") or "",
                f.get("statut") or "",
                f.get("capaciteMax"),
            ),
        )
    conn.commit()
    return len(data)


def load_inscriptions(conn: sqlite3.Connection) -> int:
    """Charge les inscriptions : on récupère par formation ou en-attente si dispo."""
    # L'API peut exposer GET /api/inscriptions (toutes) selon le backend.
    # Sinon on itère sur les formations et on appelle getByFormation.
    formations = api_get("/api/formations")
    if not isinstance(formations, list):
        return 0
    conn.execute("DELETE FROM inscriptions")
    count = 0
    for f in formations:
        fid = f.get("id")
        if not fid:
            continue
        data = api_get(f"/api/inscriptions/formation/{fid}")
        if not isinstance(data, list):
            continue
        for i in data:
            iid = i.get("id")
            if iid is None:
                continue
            conn.execute(
                """INSERT OR REPLACE INTO inscriptions (id, freelancer_id, formation_id, formation_titre, statut, date_inscription)
                   VALUES (?, ?, ?, ?, ?, ?)""",
                (
                    iid,
                    i.get("freelancerId"),
                    i.get("formationId") or fid,
                    i.get("formationTitre") or f.get("titre"),
                    i.get("statut") or "",
                    i.get("dateInscription") or "",
                ),
            )
            count += 1
    conn.commit()
    return count


def load_examens(conn: sqlite3.Connection) -> int:
    """Charge les examens (Evaluation) par formation."""
    formations = api_get("/api/formations")
    if not isinstance(formations, list):
        return 0
    conn.execute("DELETE FROM examens")
    count = 0
    for f in formations:
        fid = f.get("id")
        if not fid:
            continue
        data = api_get(f"/api/examens/formation/{fid}")
        if not isinstance(data, list):
            continue
        for e in data:
            eid = e.get("id")
            if eid is None:
                continue
            conn.execute(
                """INSERT OR REPLACE INTO examens (id, formation_id, titre, description, seuil_reussi)
                   VALUES (?, ?, ?, ?, ?)""",
                (
                    eid,
                    fid,
                    e.get("titre") or "",
                    e.get("description") or "",
                    e.get("seuilReussi") or 0,
                ),
            )
            count += 1
    conn.commit()
    return count


def load_passages_and_certificats(conn: sqlite3.Connection) -> tuple[int, int]:
    """Charge passages (résultats) et certificats par freelancer."""
    users = api_get("/api/users")
    if not isinstance(users, list):
        return 0, 0
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    conn.execute("DELETE FROM passages")
    conn.execute("DELETE FROM certificats")
    pc, cc = 0, 0
    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        resultats = api_get(f"/api/examens/resultats/freelancer/{fid}")
        if isinstance(resultats, list):
            for r in resultats:
                rid = r.get("id")
                if rid is None:
                    continue
                conn.execute(
                    """INSERT OR REPLACE INTO passages (id, freelancer_id, examen_id, examen_titre, score, resultat, date_passage)
                       VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    (
                        rid,
                        fid,
                        r.get("examenId"),
                        r.get("examenTitre") or "",
                        r.get("score") or 0,
                        r.get("resultat") or "",
                        r.get("datePassage") or "",
                    ),
                )
                pc += 1
        certs = api_get(f"/api/certificats/freelancer/{fid}")
        if isinstance(certs, list):
            for c in certs:
                cid = c.get("id")
                if cid is None:
                    continue
                conn.execute(
                    """INSERT OR REPLACE INTO certificats (id, passage_examen_id, freelancer_id, examen_id, examen_titre, numero_certificat, score, date_passage, date_delivrance)
                       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    (
                        cid,
                        c.get("passageExamenId"),
                        c.get("freelancerId") or fid,
                        c.get("examenId"),
                        c.get("examenTitre") or "",
                        c.get("numeroCertificat") or "",
                        c.get("score") or 0,
                        c.get("datePassage") or "",
                        c.get("dateDelivrance") or "",
                    ),
                )
                cc += 1
    conn.commit()
    return pc, cc


def load_skills(conn: sqlite3.Connection) -> int:
    """Charge les compétences (Skill) : liste globale si dispo, sinon par freelancer."""
    data = api_get("/api/skills")
    if isinstance(data, list):
        conn.execute("DELETE FROM skills")
        for s in data:
            sid = s.get("id")
            if sid is None:
                continue
            conn.execute(
                """INSERT OR REPLACE INTO skills (id, name, category, freelancer_id, level, years_of_experience, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (
                    sid,
                    s.get("name") or "",
                    s.get("category") or "",
                    s.get("freelancerId"),
                    s.get("level") or "",
                    s.get("yearsOfExperience"),
                    s.get("createdAt") or "",
                ),
            )
        conn.commit()
        return len(data)
    # Fallback : par freelancer
    users = api_get("/api/users")
    if not isinstance(users, list):
        return 0
    freelancers = [u for u in users if u.get("role") == "FREELANCER"]
    conn.execute("DELETE FROM skills")
    count = 0
    for f in freelancers:
        fid = f.get("id") or f.get("userId")
        if not fid:
            continue
        data = api_get(f"/api/skills/freelancer/{fid}")
        if not isinstance(data, list):
            continue
        for s in data:
            sid = s.get("id")
            if sid is None:
                continue
            conn.execute(
                """INSERT OR REPLACE INTO skills (id, name, category, freelancer_id, level, years_of_experience, created_at)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (
                    sid,
                    s.get("name") or "",
                    s.get("category") or "",
                    s.get("freelancerId") or fid,
                    s.get("level") or "",
                    s.get("yearsOfExperience"),
                    s.get("createdAt") or "",
                ),
            )
            count += 1
    conn.commit()
    return count


def run_etl(full_refresh: bool = True) -> dict:
    """
    Exécute le pipeline ETL complet.
    full_refresh=True : recrée les tables et recharge tout.
    Retourne un résumé des lignes chargées.
    """
    ensure_dir(ETL_DB_PATH)
    conn = sqlite3.connect(ETL_DB_PATH)
    create_schema(conn)
    summary = {}
    try:
        summary["users"] = load_users(conn)
        summary["formations"] = load_formations(conn)
        summary["inscriptions"] = load_inscriptions(conn)
        summary["examens"] = load_examens(conn)
        passages, certs = load_passages_and_certificats(conn)
        summary["passages"] = passages
        summary["certificats"] = certs
        summary["skills"] = load_skills(conn)
    finally:
        conn.close()
    return summary


if __name__ == "__main__":
    summary = run_etl()
    print("[ETL] Terminé:", summary)
    print(f"[ETL] Base: {ETL_DB_PATH}")

"""
ETL & Analytics : indicateurs par formation et par freelancer,
export CSV et graphiques (matplotlib).
À lancer après etl.py (python etl.py puis python analytics.py).
Les rapports sont écrits dans le dossier reports/ (CSV + PNG),
puis copiés dans frontend/public/reports/ pour affichage sur la plateforme.
"""
import csv
import shutil
import sqlite3
import sys
from pathlib import Path

from config import ETL_DB_PATH

REPORTS_DIR = Path(__file__).resolve().parent / "reports"

# Dossier côté frontend pour afficher les graphiques sur la plateforme (Angular)
FRONTEND_REPORTS_DIR = Path(__file__).resolve().parent.parent / "frontend" / "public" / "reports"


def ensure_reports_dir() -> None:
    REPORTS_DIR.mkdir(parents=True, exist_ok=True)


def get_conn() -> sqlite3.Connection | None:
    if not Path(ETL_DB_PATH).exists():
        print("[Analytics] Base ETL absente. Lancez d'abord : python etl.py", file=sys.stderr)
        return None
    return sqlite3.connect(ETL_DB_PATH)


def indicateurs_formations(conn: sqlite3.Connection) -> list[dict]:
    """Taux d'inscription, passage, réussite par formation."""
    rows = conn.execute("""
        SELECT
            f.id,
            f.titre,
            f.type_formation,
            f.capacite_max,
            f.statut,
            f.date_fin,
            (SELECT COUNT(*) FROM inscriptions i WHERE i.formation_id = f.id) AS nb_inscriptions,
            (SELECT COUNT(*) FROM inscriptions i WHERE i.formation_id = f.id AND i.statut = 'VALIDEE') AS nb_validees,
            (SELECT COUNT(*) FROM passages p
             JOIN examens e ON e.id = p.examen_id AND e.formation_id = f.id) AS nb_passages,
            (SELECT COUNT(*) FROM certificats c
             JOIN examens e ON e.id = c.examen_id AND e.formation_id = f.id) AS nb_certificats
        FROM formations f
        ORDER BY f.id
    """).fetchall()
    out = []
    for r in rows:
        fid, titre, type_f, cap_max, statut, date_fin, nb_ins, nb_val, nb_pass, nb_cert = r
        cap = cap_max or 1
        taux_inscription = round(100 * nb_ins / cap, 1) if cap else 0
        taux_passage = round(100 * nb_pass / nb_val, 1) if nb_val else 0
        taux_reussite = round(100 * nb_cert / nb_pass, 1) if nb_pass else 0
        out.append({
            "formation_id": fid,
            "titre": titre or "",
            "type_formation": type_f or "",
            "capacite_max": cap_max,
            "statut": statut or "",
            "date_fin": date_fin or "",
            "nb_inscriptions": nb_ins,
            "nb_inscriptions_validees": nb_val,
            "nb_passages_examen": nb_pass,
            "nb_certificats": nb_cert,
            "taux_inscription_pct": taux_inscription,
            "taux_passage_pct": taux_passage,
            "taux_reussite_pct": taux_reussite,
        })
    return out


def indicateurs_freelancers(conn: sqlite3.Connection) -> list[dict]:
    """Nombre de certificats, formations suivies, score moyen par freelancer."""
    rows = conn.execute("""
        SELECT
            u.id,
            u.email,
            u.full_name,
            (SELECT COUNT(DISTINCT i.formation_id) FROM inscriptions i WHERE i.freelancer_id = u.id AND i.statut = 'VALIDEE') AS nb_formations,
            (SELECT COUNT(*) FROM certificats c WHERE c.freelancer_id = u.id) AS nb_certificats,
            (SELECT COALESCE(AVG(p.score), 0) FROM passages p WHERE p.freelancer_id = u.id) AS score_moyen
        FROM users u
        WHERE u.role = 'FREELANCER'
        ORDER BY nb_certificats DESC, score_moyen DESC
    """).fetchall()
    out = []
    for r in rows:
        uid, email, full_name, nb_form, nb_cert, score_moy = r
        out.append({
            "freelancer_id": uid,
            "email": email or "",
            "full_name": full_name or "",
            "nb_formations_suivies": nb_form or 0,
            "nb_certificats": nb_cert or 0,
            "score_moyen_examens": round(score_moy or 0, 1),
        })
    return out


def export_csv_indicateurs_formations(data: list[dict], path: Path) -> None:
    if not data:
        return
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=data[0].keys(), delimiter=";")
        w.writeheader()
        w.writerows(data)
    print(f"[Analytics] CSV écrit : {path}")


def export_csv_indicateurs_freelancers(data: list[dict], path: Path) -> None:
    if not data:
        return
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=data[0].keys(), delimiter=";")
        w.writeheader()
        w.writerows(data)
    print(f"[Analytics] CSV écrit : {path}")


def graphiques(conn: sqlite3.Connection) -> None:
    """Génère des graphiques PNG dans reports/."""
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        print("[Analytics] matplotlib non installé. Pip install matplotlib pour les graphiques.", file=sys.stderr)
        return

    ensure_reports_dir()

    # 1) Inscriptions par formation (top 10)
    rows = conn.execute("""
        SELECT f.titre, COUNT(i.id) AS nb
        FROM formations f
        LEFT JOIN inscriptions i ON i.formation_id = f.id
        GROUP BY f.id
        ORDER BY nb DESC
        LIMIT 10
    """).fetchall()
    if rows:
        titres = [r[0][:20] + "…" if len(r[0] or "") > 20 else (r[0] or "?") for r in rows]
        nb = [r[1] for r in rows]
        fig, ax = plt.subplots(figsize=(10, 5))
        ax.barh(range(len(titres)), nb, color="teal", alpha=0.8)
        ax.set_yticks(range(len(titres)))
        ax.set_yticklabels(titres, fontsize=9)
        ax.set_xlabel("Nombre d'inscriptions")
        ax.set_title("Top 10 formations — Inscriptions")
        fig.tight_layout()
        fig.savefig(REPORTS_DIR / "graph_inscriptions_par_formation.png", dpi=120, bbox_inches="tight")
        plt.close()
        print(f"[Analytics] Graphique écrit : {REPORTS_DIR / 'graph_inscriptions_par_formation.png'}")

    # 2) Certificats délivrés par mois
    rows = conn.execute("""
        SELECT strftime('%Y-%m', date_delivrance) AS mois, COUNT(*) AS nb
        FROM certificats
        WHERE date_delivrance IS NOT NULL AND date_delivrance != ''
        GROUP BY mois
        ORDER BY mois
    """).fetchall()
    if rows:
        mois = [r[0] for r in rows]
        nb = [r[1] for r in rows]
        fig, ax = plt.subplots(figsize=(8, 4))
        ax.plot(range(len(mois)), nb, marker="o", color="coral", linewidth=2)
        ax.set_xticks(range(len(mois)))
        ax.set_xticklabels(mois, rotation=45, ha="right")
        ax.set_ylabel("Nombre de certificats")
        ax.set_title("Certificats délivrés par mois")
        fig.tight_layout()
        fig.savefig(REPORTS_DIR / "graph_certificats_par_mois.png", dpi=120, bbox_inches="tight")
        plt.close()
        print(f"[Analytics] Graphique écrit : {REPORTS_DIR / 'graph_certificats_par_mois.png'}")


def copy_reports_to_frontend() -> None:
    """Copie les PNG et CSV vers frontend/public/reports/ pour affichage sur la plateforme."""
    if not FRONTEND_REPORTS_DIR.parent.exists():
        return
    FRONTEND_REPORTS_DIR.mkdir(parents=True, exist_ok=True)
    for name in [
        "graph_inscriptions_par_formation.png",
        "graph_certificats_par_mois.png",
        "indicateurs_formations.csv",
        "indicateurs_freelancers.csv",
    ]:
        src = REPORTS_DIR / name
        if src.exists():
            shutil.copy2(src, FRONTEND_REPORTS_DIR / name)
    print(f"[Analytics] Rapports copiés vers la plateforme : {FRONTEND_REPORTS_DIR}")


def run_analytics() -> dict:
    """Lance le calcul des indicateurs, export CSV et graphiques."""
    ensure_reports_dir()
    conn = get_conn()
    if not conn:
        return {}
    try:
        ind_form = indicateurs_formations(conn)
        ind_free = indicateurs_freelancers(conn)
        export_csv_indicateurs_formations(ind_form, REPORTS_DIR / "indicateurs_formations.csv")
        export_csv_indicateurs_freelancers(ind_free, REPORTS_DIR / "indicateurs_freelancers.csv")
        graphiques(conn)
        copy_reports_to_frontend()
        return {
            "indicateurs_formations": len(ind_form),
            "indicateurs_freelancers": len(ind_free),
            "reports_dir": str(REPORTS_DIR),
        }
    finally:
        conn.close()


if __name__ == "__main__":
    print("[Analytics] Utilisation de la base :", ETL_DB_PATH)
    summary = run_analytics()
    if summary:
        print("[Analytics] Terminé :", summary)
        print("[Analytics] Ouvrez le dossier 'reports' pour voir les CSV et graphiques.")

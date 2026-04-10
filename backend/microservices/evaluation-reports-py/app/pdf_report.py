"""Rapport PDF : ReportLab + images PNG (Pillow), avec résumé narratif exécutif."""

from __future__ import annotations

from io import BytesIO
import statistics
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Image, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from app.charts import evolution_png, histogram_png


def _mean_std(scores: list[float]) -> tuple[float, float]:
    m = statistics.mean(scores)
    if len(scores) < 2:
        return m, 0.0
    return m, statistics.pstdev(scores)


def _trend_delta(values: list[float]) -> float:
    if len(values) < 2:
        return 0.0
    return values[-1] - values[0]


def build_executive_summary(scores: list[float], evolution_values: list[float]) -> dict:
    """Génère un résumé narratif : points forts/faibles + actions recommandées."""
    mean_s, std_s = _mean_std(scores)
    success_threshold = 70.0
    success_rate = (sum(1 for s in scores if s >= success_threshold) / len(scores)) * 100.0
    trend = _trend_delta(evolution_values)

    strengths: list[str] = []
    weaknesses: list[str] = []
    actions: list[str] = []

    if mean_s >= 75:
        strengths.append(f"Niveau global satisfaisant (moyenne {mean_s:.1f}/100).")
    elif mean_s >= 65:
        strengths.append(f"Niveau intermédiaire stable (moyenne {mean_s:.1f}/100).")
    else:
        weaknesses.append(f"Niveau global faible (moyenne {mean_s:.1f}/100).")

    if success_rate >= 75:
        strengths.append(f"Bon taux de réussite ({success_rate:.0f}%).")
    elif success_rate < 55:
        weaknesses.append(f"Taux de réussite insuffisant ({success_rate:.0f}%).")

    if std_s <= 10:
        strengths.append("Résultats homogènes entre participants (dispersion faible).")
    elif std_s >= 16:
        weaknesses.append("Résultats hétérogènes (écarts importants entre participants).")

    if trend >= 4:
        strengths.append(f"Progression positive observée (+{trend:.1f} pts sur la période).")
    elif trend <= -4:
        weaknesses.append(f"Tendance baissière à surveiller ({trend:.1f} pts).")

    if not strengths:
        strengths.append("Des points de progression existent, mais la base de résultats est exploitable.")

    if not weaknesses:
        weaknesses.append("Aucun signal critique détecté sur cet échantillon.")

    if mean_s < 70:
        actions.append("Renforcer les modules de remédiation sur les notions fondamentales avant la prochaine session.")
    if std_s >= 16:
        actions.append("Segmenter les apprenants par niveau et proposer des parcours différenciés.")
    if trend <= -4:
        actions.append("Analyser les changements récents (contenu, consignes, difficulté) et recalibrer l'examen.")
    if success_rate < 55:
        actions.append("Revoir le seuil de validation et la clarté des questions les moins réussies.")

    if not actions:
        actions.append("Maintenir le dispositif actuel et suivre l'indicateur sur les deux prochaines sessions.")

    narrative = (
        f"Synthèse exécutive : moyenne {mean_s:.1f}/100, taux de réussite {success_rate:.0f}%, "
        f"dispersion {std_s:.1f}, tendance {'haussière' if trend > 1 else 'baissière' if trend < -1 else 'stable'}."
    )

    return {
        "overview": {
            "mean": round(mean_s, 2),
            "std_dev": round(std_s, 2),
            "success_rate": round(success_rate, 2),
            "trend_delta": round(trend, 2),
            "sample_size": len(scores),
        },
        "strengths": strengths,
        "weaknesses": weaknesses,
        "recommended_actions": actions,
        "narrative": narrative,
    }


def build_dashboard_pdf(
    title: str,
    subtitle: str,
    scores: list[float],
    evolution_labels: list[str],
    evolution_values: list[float],
) -> bytes:
    """PDF : page titre + histogramme + courbe + tableau + résumé exécutif."""
    hist_bytes = histogram_png(scores, title="Histogramme des notes")
    evo_bytes = evolution_png(evolution_labels, evolution_values, title="Évolution")
    summary = build_executive_summary(scores, evolution_values)

    buf = BytesIO()
    doc = SimpleDocTemplate(buf, pagesize=A4, rightMargin=2 * cm, leftMargin=2 * cm, topMargin=2 * cm, bottomMargin=2 * cm)
    styles = getSampleStyleSheet()
    story = []

    story.append(Paragraph(f"<b>{escape(title)}</b>", styles["Title"]))
    story.append(Spacer(1, 0.4 * cm))
    story.append(Paragraph(escape(subtitle), styles["Normal"]))
    story.append(Spacer(1, 0.5 * cm))
    story.append(Paragraph(escape(summary["narrative"]), styles["Normal"]))
    story.append(Spacer(1, 0.8 * cm))

    story.append(Paragraph("<b>Histogramme</b>", styles["Heading2"]))
    story.append(Spacer(1, 0.2 * cm))
    story.append(Image(BytesIO(hist_bytes), width=16 * cm, height=10 * cm))
    story.append(Spacer(1, 0.6 * cm))

    story.append(Paragraph("<b>Courbe d'évolution</b>", styles["Heading2"]))
    story.append(Spacer(1, 0.2 * cm))
    story.append(Image(BytesIO(evo_bytes), width=16 * cm, height=10 * cm))
    story.append(Spacer(1, 0.6 * cm))

    mean_s, std_s = _mean_std(scores)
    data = [
        ["Indicateur", "Valeur"],
        ["Nombre de notes", str(len(scores))],
        ["Moyenne", f"{mean_s:.2f}"],
        ["Écart-type", f"{std_s:.2f}"],
        ["Taux de réussite (>=70)", f"{summary['overview']['success_rate']:.1f}%"],
        ["Tendance (dernier-premier)", f"{summary['overview']['trend_delta']:+.2f}"],
    ]
    t = Table(data, colWidths=[8 * cm, 6 * cm])
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1e40af")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 10),
                ("GRID", (0, 0), (-1, -1), 0.5, colors.grey),
                ("ROWBACKGROUND", (0, 1), (-1, -1), colors.beige),
            ]
        )
    )
    story.append(Paragraph("<b>Synthèse statistique</b>", styles["Heading2"]))
    story.append(Spacer(1, 0.3 * cm))
    story.append(t)
    story.append(Spacer(1, 0.5 * cm))

    story.append(Paragraph("<b>Résumé exécutif automatique</b>", styles["Heading2"]))
    story.append(Spacer(1, 0.15 * cm))
    story.append(Paragraph("<b>Points forts</b>", styles["Normal"]))
    for item in summary["strengths"]:
        story.append(Paragraph(escape(f"- {item}"), styles["Normal"]))
    story.append(Spacer(1, 0.15 * cm))
    story.append(Paragraph("<b>Points faibles</b>", styles["Normal"]))
    for item in summary["weaknesses"]:
        story.append(Paragraph(escape(f"- {item}"), styles["Normal"]))
    story.append(Spacer(1, 0.15 * cm))
    story.append(Paragraph("<b>Actions recommandées</b>", styles["Normal"]))
    for item in summary["recommended_actions"]:
        story.append(Paragraph(escape(f"- {item}"), styles["Normal"]))

    doc.build(story)
    buf.seek(0)
    return buf.read()

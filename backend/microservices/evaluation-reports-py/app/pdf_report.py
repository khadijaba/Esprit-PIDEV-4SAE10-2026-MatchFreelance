"""Rapport PDF : ReportLab + images PNG (Pillow), avec résumé narratif exécutif."""

from __future__ import annotations

from datetime import datetime
from io import BytesIO
from pathlib import Path
import statistics
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Image, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from app.charts import evolution_png, histogram_png

# Charte MatchFreelance (PDF)
_LOGO_PATH = Path(__file__).resolve().parent / "assets" / "logo.png"
_PAGE_W, _PAGE_H = A4
_MARGIN_X = 2 * cm
_MARGIN_TOP = 1.9 * cm
_MARGIN_BOTTOM = 2.4 * cm
_CONTENT_WIDTH = _PAGE_W - 2 * _MARGIN_X

_MF_BLUE = colors.HexColor("#1e40af")
_MF_BLUE_DARK = colors.HexColor("#1e3a8a")
_MF_ORANGE = colors.HexColor("#ea580c")
_MF_SLATE = colors.HexColor("#334155")
_MF_SLATE_MUTED = colors.HexColor("#64748b")
_MF_SURFACE = colors.HexColor("#f8fafc")
_MF_BORDER = colors.HexColor("#e2e8f0")
_MF_OK_BG = colors.HexColor("#ecfdf5")
_MF_WARN_BG = colors.HexColor("#fffbeb")
_MF_INFO_BG = colors.HexColor("#eff6ff")


def _mf_styles():
    s = getSampleStyleSheet()
    s.add(
        ParagraphStyle(
            name="MFDocTitle",
            parent=s["Title"],
            fontName="Helvetica-Bold",
            fontSize=17,
            leading=21,
            textColor=_MF_BLUE_DARK,
            spaceAfter=4,
            alignment=TA_CENTER,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFSubtitle",
            parent=s["Normal"],
            fontSize=10.5,
            leading=14,
            textColor=_MF_SLATE_MUTED,
            spaceAfter=6,
            alignment=TA_CENTER,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFMeta",
            parent=s["Normal"],
            fontSize=8,
            leading=11,
            textColor=_MF_SLATE_MUTED,
            spaceAfter=14,
            alignment=TA_CENTER,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFNarrative",
            parent=s["Normal"],
            fontSize=10,
            leading=14,
            textColor=_MF_SLATE,
            alignment=TA_JUSTIFY,
            spaceAfter=0,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFSectionTitle",
            parent=s["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=11,
            leading=14,
            textColor=_MF_BLUE_DARK,
            spaceBefore=10,
            spaceAfter=6,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFSectionBar",
            parent=s["MFSectionTitle"],
            spaceBefore=2,
            spaceAfter=0,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFCaption",
            parent=s["Normal"],
            fontSize=8.5,
            leading=11,
            textColor=_MF_SLATE_MUTED,
            alignment=TA_CENTER,
            spaceAfter=10,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFBody",
            parent=s["Normal"],
            fontSize=9.5,
            leading=13,
            textColor=_MF_SLATE,
        )
    )
    s.add(
        ParagraphStyle(
            name="MFExecHeading",
            parent=s["Normal"],
            fontName="Helvetica-Bold",
            fontSize=9.5,
            leading=12,
            textColor=_MF_BLUE_DARK,
            spaceAfter=4,
        )
    )
    return s


def _section_bar(title: str, styles) -> Table:
    """Bandeau de section : accent orange + titre."""
    bar = Table(
        [[Paragraph(escape(title), styles["MFSectionBar"])]],
        colWidths=[_CONTENT_WIDTH],
    )
    bar.setStyle(
        TableStyle(
            [
                ("LINEABOVE", (0, 0), (-1, -1), 2, _MF_ORANGE),
                ("LINEBELOW", (0, 0), (-1, -1), 0.5, _MF_BORDER),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                ("LEFTPADDING", (0, 0), (-1, -1), 0),
            ]
        )
    )
    return bar


def _figure_with_caption(img: Image, caption: str, styles) -> list:
    img.hAlign = "CENTER"
    cap = Paragraph(escape(caption), styles["MFCaption"])
    box = Table([[img]], colWidths=[_CONTENT_WIDTH])
    box.setStyle(
        TableStyle(
            [
                ("BOX", (0, 0), (-1, -1), 0.75, _MF_BORDER),
                ("BACKGROUND", (0, 0), (-1, -1), colors.white),
                ("TOPPADDING", (0, 0), (-1, -1), 8),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("ALIGN", (0, 0), (-1, -1), "CENTER"),
            ]
        )
    )
    return [box, cap]


def _insight_box(title: str, body_paras: list[Paragraph], bg: colors.Color, styles) -> Table:
    """Bloc encadré (synthèse, listes exécutives)."""
    rows = [[Paragraph(escape(title), styles["MFExecHeading"])]]
    for p in body_paras:
        rows.append([p])
    t = Table(rows, colWidths=[_CONTENT_WIDTH])
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, -1), bg),
                ("BOX", (0, 0), (-1, -1), 0.75, _MF_BORDER),
                ("TOPPADDING", (0, 0), (-1, -1), 10),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 10),
                ("LEFTPADDING", (0, 0), (-1, -1), 12),
                ("RIGHTPADDING", (0, 0), (-1, -1), 12),
            ]
        )
    )
    return t


def _exec_list_block(heading: str, items: list[str], bg: colors.Color, styles) -> Table:
    bullets = [Paragraph(escape(f"• {it}"), styles["MFBody"]) for it in items]
    return _insight_box(heading, bullets, bg, styles)


def _draw_page_frame(canvas, doc) -> None:
    """Filet en tête + pied de page (repères lecture décideur)."""
    canvas.saveState()
    canvas.setStrokeColor(_MF_BORDER)
    canvas.setLineWidth(0.6)
    y_rule = _PAGE_H - 1.0 * cm
    canvas.line(_MARGIN_X, y_rule, _PAGE_W - _MARGIN_X, y_rule)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(_MF_SLATE_MUTED)
    y_foot = 0.85 * cm
    canvas.drawString(_MARGIN_X, y_foot, "MatchFreelance — Connecting Skills to Opportunities")
    canvas.drawRightString(_PAGE_W - _MARGIN_X, y_foot, f"Document confidentiel · page {canvas.getPageNumber()}")
    canvas.restoreState()


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
    """PDF professionnel : couverture, visualisations encadrées, tableau KPI, résumé exécutif."""
    hist_bytes = histogram_png(scores, title="Histogramme des notes")
    evo_bytes = evolution_png(evolution_labels, evolution_values, title="Évolution")
    summary = build_executive_summary(scores, evolution_values)
    styles = _mf_styles()
    generated = datetime.now().strftime("%d/%m/%Y à %H:%M")

    buf = BytesIO()
    doc = SimpleDocTemplate(
        buf,
        pagesize=A4,
        rightMargin=_MARGIN_X,
        leftMargin=_MARGIN_X,
        topMargin=_MARGIN_TOP,
        bottomMargin=_MARGIN_BOTTOM,
        title=escape(title),
        author="MatchFreelance",
    )
    story = []

    # --- Couverture (logo centré + titres) ---
    if _LOGO_PATH.is_file():
        logo = Image(str(_LOGO_PATH), width=min(9.5 * cm, _CONTENT_WIDTH * 0.72))
        logo.hAlign = "CENTER"
        logo_row = Table([[logo]], colWidths=[_CONTENT_WIDTH])
        logo_row.setStyle(
            TableStyle(
                [
                    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                    ("TOPPADDING", (0, 0), (-1, -1), 4),
                    ("BOTTOMPADDING", (0, 0), (-1, -1), 14),
                ]
            )
        )
        story.append(logo_row)

    story.append(Paragraph(escape(title), styles["MFDocTitle"]))
    story.append(Paragraph(escape(subtitle), styles["MFSubtitle"]))
    story.append(
        Paragraph(
            escape(f"Rapport d'évaluation · généré le {generated} · usage interne"),
            styles["MFMeta"],
        )
    )

    narrative_box = _insight_box(
        "Synthèse exécutive (aperçu)",
        [Paragraph(escape(summary["narrative"]), styles["MFNarrative"])],
        _MF_SURFACE,
        styles,
    )
    story.append(narrative_box)
    story.append(Spacer(1, 0.55 * cm))

    # --- Visualisations ---
    chart_w = min(_CONTENT_WIDTH - 1.6 * cm, 16 * cm)
    chart_h = chart_w * (10 / 16)
    story.append(_section_bar("Indicateurs visuels", styles))
    hist_img = Image(BytesIO(hist_bytes), width=chart_w, height=chart_h)
    story.extend(_figure_with_caption(hist_img, "Figure 1 — Répartition des notes (histogramme)", styles))
    evo_img = Image(BytesIO(evo_bytes), width=chart_w, height=chart_h)
    story.extend(_figure_with_caption(evo_img, "Figure 2 — Évolution des résultats dans le temps", styles))

    # --- Tableau KPI ---
    mean_s, std_s = _mean_std(scores)
    data = [
        ["Indicateur clé", "Valeur", "Commentaire"],
        [
            "Effectif (notes)",
            str(len(scores)),
            "Nombre de copies / notes prises en compte",
        ],
        [
            "Moyenne",
            f"{mean_s:.2f} / 100",
            "Niveau global de l'échantillon",
        ],
        [
            "Écart-type",
            f"{std_s:.2f}",
            "Dispersion des résultats",
        ],
        [
            "Taux de réussite (>= 70)",
            f"{summary['overview']['success_rate']:.1f} %",
            "Part des notes au-dessus du seuil",
        ],
        [
            "Tendance (fin - debut)",
            f"{summary['overview']['trend_delta']:+.2f}",
            "Delta sur la série d'évolution",
        ],
    ]
    col_a = _CONTENT_WIDTH * 0.40
    col_b = _CONTENT_WIDTH * 0.22
    col_c = _CONTENT_WIDTH - col_a - col_b
    t = Table(data, colWidths=[col_a, col_b, col_c], repeatRows=1)
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), _MF_BLUE),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, 0), 9.5),
                ("BOTTOMPADDING", (0, 0), (-1, 0), 8),
                ("TOPPADDING", (0, 0), (-1, 0), 8),
                ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
                ("FONTSIZE", (0, 1), (-1, -1), 9),
                ("TEXTCOLOR", (0, 1), (-1, -1), _MF_SLATE),
                ("GRID", (0, 0), (-1, -1), 0.5, _MF_BORDER),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, _MF_SURFACE]),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("TOPPADDING", (0, 1), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 1), (-1, -1), 7),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
            ]
        )
    )
    story.append(_section_bar("Tableau de bord — indicateurs clés", styles))
    story.append(t)
    story.append(Spacer(1, 0.45 * cm))

    # --- Résumé décisionnel ---
    story.append(_section_bar("Analyse décisionnelle (génération automatique)", styles))
    story.append(Spacer(1, 0.15 * cm))
    story.append(_exec_list_block("Points forts", summary["strengths"], _MF_OK_BG, styles))
    story.append(Spacer(1, 0.35 * cm))
    story.append(_exec_list_block("Points de vigilance", summary["weaknesses"], _MF_WARN_BG, styles))
    story.append(Spacer(1, 0.35 * cm))
    story.append(_exec_list_block("Actions recommandées", summary["recommended_actions"], _MF_INFO_BG, styles))

    doc.build(story, onFirstPage=_draw_page_frame, onLaterPages=_draw_page_frame)
    buf.seek(0)
    return buf.read()

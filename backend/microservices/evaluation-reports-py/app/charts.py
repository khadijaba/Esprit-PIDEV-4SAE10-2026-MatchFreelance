"""Graphiques PNG : histogramme et courbe (Pillow uniquement — pas de NumPy/Matplotlib, compatible Python 3.14)."""

from __future__ import annotations

from io import BytesIO
from PIL import Image, ImageDraw, ImageFont


def _hist_counts(scores: list[float], n_bins: int) -> tuple[list[int], float, float, float]:
    lo, hi = min(scores), max(scores)
    if hi == lo:
        return [len(scores)], lo, hi, 1.0
    width = (hi - lo) / n_bins
    counts = [0] * n_bins
    for s in scores:
        i = min(int((s - lo) / width), n_bins - 1)
        counts[i] += 1
    return counts, lo, hi, width


def histogram_png(scores: list[float], title: str = "Répartition des notes") -> bytes:
    n_bins = min(12, max(5, len(scores) // 2))
    counts, lo, hi, _ = _hist_counts(scores, n_bins)
    mx = max(counts) if counts else 1

    w, h = 960, 600
    margin_l, margin_r, margin_t, margin_b = 70, 40, 70, 90
    plot_w = w - margin_l - margin_r
    plot_h = h - margin_t - margin_b

    img = Image.new("RGB", (w, h), color="#ffffff")
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 18)
        font_small = ImageFont.truetype("arial.ttf", 14)
    except OSError:
        font = ImageFont.load_default()
        font_small = font

    draw.rectangle([0, 0, w, h], fill="#ffffff")
    draw.text((margin_l, 20), title, fill="#111827", font=font)

    bar_w = plot_w / n_bins
    for i, c in enumerate(counts):
        bh = (c / mx) * plot_h if mx else 0
        x0 = margin_l + i * bar_w + 2
        x1 = margin_l + (i + 1) * bar_w - 2
        y0 = margin_t + plot_h - bh
        y1 = margin_t + plot_h
        draw.rectangle([x0, y0, x1, y1], fill="#2563eb", outline="#1d4ed8")

    draw.line([margin_l, margin_t + plot_h, margin_l + plot_w, margin_t + plot_h], fill="#374151", width=2)
    draw.line([margin_l, margin_t, margin_l, margin_t + plot_h], fill="#374151", width=2)

    for i in range(n_bins + 1):
        x = margin_l + (i / n_bins) * plot_w
        tick = lo + (hi - lo) * (i / n_bins) if hi > lo else lo
        draw.line([x, margin_t + plot_h, x, margin_t + plot_h + 5], fill="#374151")
        draw.text((x - 12, margin_t + plot_h + 8), f"{tick:.1f}", fill="#6b7280", font=font_small)

    draw.text((margin_l, h - 35), "Note", fill="#374151", font=font_small)
    draw.text((20, margin_t + plot_h // 2), "Effectif", fill="#374151", font=font_small)

    buf = BytesIO()
    img.save(buf, format="PNG", optimize=True)
    buf.seek(0)
    return buf.read()


def evolution_png(labels: list[str], values: list[float], title: str = "Évolution des résultats") -> bytes:
    w, h = 1080, 600
    margin_l, margin_r, margin_t, margin_b = 80, 40, 70, 120
    plot_w = w - margin_l - margin_r
    plot_h = h - margin_t - margin_b

    img = Image.new("RGB", (w, h), color="#ffffff")
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arial.ttf", 18)
        font_small = ImageFont.truetype("arial.ttf", 13)
    except OSError:
        font = ImageFont.load_default()
        font_small = font

    draw.text((margin_l, 20), title, fill="#111827", font=font)

    vmin, vmax = min(values), max(values)
    if vmax == vmin:
        vmin, vmax = vmin - 1, vmax + 1
    pad = (vmax - vmin) * 0.1 or 0.5
    y0, y1 = vmin - pad, vmax + pad

    n = len(values)
    pts = []
    for i, v in enumerate(values):
        x = margin_l + (i / max(n - 1, 1)) * plot_w
        t = (v - y0) / (y1 - y0)
        y = margin_t + plot_h - t * plot_h
        pts.append((x, y))

    draw.line([margin_l, margin_t + plot_h, margin_l + plot_w, margin_t + plot_h], fill="#374151", width=2)
    draw.line([margin_l, margin_t, margin_l, margin_t + plot_h], fill="#374151", width=2)

    for i in range(len(pts) - 1):
        draw.line([pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1]], fill="#059669", width=3)
    for x, y in pts:
        r = 6
        draw.ellipse([x - r, y - r, x + r, y + r], fill="#059669", outline="#047857")

    for i, label in enumerate(labels):
        x = margin_l + (i / max(n - 1, 1)) * plot_w
        draw.text((x - 15, margin_t + plot_h + 10), str(label)[:12], fill="#4b5563", font=font_small)

    buf = BytesIO()
    img.save(buf, format="PNG", optimize=True)
    buf.seek(0)
    return buf.read()

"""
Entraîne un classifieur binaire "risque projet élevé" et exporte project-risk.onnx
pour le microservice Java (ONNX Runtime).

Les7 features DOIVENT rester alignées avec ProjectRiskFeatureVector.java (ordre identique).

Usage (depuis ce dossier ml/) :
  pip install -r requirements.txt
  python train_project_risk.py

Remplacez ensuite les données synthétiques par un export CSV réel (labels = incidents métier).
"""
from __future__ import annotations

import json
from pathlib import Path

import numpy as np
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

FEATURE_DIM = 7
# Doit correspondre à ProjectMlRiskService.MODEL_ID
MODEL_ID = "onnx-rf-risk-v1"

SCRIPT_DIR = Path(__file__).resolve().parent
RESOURCES_ML = SCRIPT_DIR.parent / "src" / "main" / "resources" / "ml"


def feature_row(
    title_len: int,
    desc_words: int,
    skill_count: int,
    budget: float,
    duration: int,
    daily: float,
    owner_completion_rate: float,
) -> np.ndarray:
    return np.array(
        [
            [
                title_len / 100.0,
                desc_words / 200.0,
                skill_count / 10.0,
                min(daily / 200.0, 2.0) / 2.0,
                float(np.log1p(budget)) / 15.0,
                min(duration / 365.0, 1.0),
                float(owner_completion_rate),
            ]
        ],
        dtype=np.float32,
    )


def synthesize(n: int, seed: int = 42) -> tuple[np.ndarray, np.ndarray]:
    rng = np.random.default_rng(seed)
    X = np.zeros((n, FEATURE_DIM), dtype=np.float32)
    y = np.zeros(n, dtype=np.int64)
    for i in range(n):
        title_len = int(rng.integers(5, 90))
        desc_words = int(rng.integers(8, 220))
        skill_count = int(rng.integers(0, 11))
        budget = float(rng.uniform(300, 80_000))
        duration = int(rng.integers(7, 200))
        daily = budget / max(1, duration)
        owner_total = int(rng.integers(1, 25))
        owner_done = int(rng.integers(0, owner_total + 1))
        completion = owner_done / owner_total

        row = feature_row(
            title_len, desc_words, skill_count, budget, duration, daily, completion
        )
        X[i] = row

        high = (
            desc_words < 36
            or skill_count == 0
            or skill_count > 8
            or daily < 10.5
            or daily > 275
            or title_len < 10
            or rng.random() < 0.07
        )
        y[i] = 1 if high else 0

    return X, y


def main() -> None:
    X, y = synthesize(4_000)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    clf = RandomForestClassifier(
        n_estimators=120,
        max_depth=8,
        min_samples_leaf=4,
        class_weight="balanced",
        random_state=42,
        n_jobs=-1,
    )
    pipe = Pipeline(
        [
            ("scaler", StandardScaler()),
            ("rf", clf),
        ]
    )
    pipe.fit(X_train, y_train)
    acc = float((pipe.predict(X_test) == y_test).mean())
    print(f"Hold-out accuracy (synthetic): {acc:.3f}")

    initial_types = [("float_input", FloatTensorType([None, FEATURE_DIM]))]
    options = {id(pipe): {"zipmap": False}}
    onnx_model = convert_sklearn(
        pipe, initial_types=initial_types, target_opset=12, options=options
    )

    RESOURCES_ML.mkdir(parents=True, exist_ok=True)
    out_onnx = RESOURCES_ML / "project-risk.onnx"
    out_meta = RESOURCES_ML / "project-risk-model.json"
    with open(out_onnx, "wb") as f:
        f.write(onnx_model.SerializeToString())

    meta = {
        "modelId": MODEL_ID,
        "featureDim": FEATURE_DIM,
        "featureNames": [
            "title_len_over_100",
            "desc_words_over_200",
            "skills_over_10",
            "daily_rate_capped_norm",
            "log1p_budget_over_15",
            "duration_over_365_cap",
            "owner_completion_rate",
        ],
        "trainingNote": "Synthetic labels from rule-like generator; replace with real outcomes.",
    }
    with open(out_meta, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)

    print(f"Wrote {out_onnx}")
    print(f"Wrote {out_meta}")


if __name__ == "__main__":
    main()

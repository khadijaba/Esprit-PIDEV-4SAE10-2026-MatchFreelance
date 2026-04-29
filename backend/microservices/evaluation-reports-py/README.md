# evaluation-reports-py

Microservice **Python (FastAPI)** : histogrammes, courbe d’évolution, export **PDF** (Pillow + ReportLab, sans compilation C).

## Python

Le service utilise **Pillow + ReportLab** (pas NumPy/Matplotlib) pour éviter toute **compilation C** sur Windows — compatible **Python 3.12 à 3.14**.

Toujours lancer depuis le dossier du projet : `cd ...\evaluation-reports-py` puis `pip install -r requirements.txt` (sinon *requirements.txt introuvable*).

## Démarrage local

```powershell
cd C:\Users\benay\Downloads\validation\backend\microservices\evaluation-reports-py
py -3 -m venv .venv
.\.venv\Scripts\activate
python -m pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8090
```

Sous **Windows**, si vous voyez `WinError 10013` avec `--host 0.0.0.0`, utilisez **`127.0.0.1`** (c’est ce que fait `run.ps1`) ou changez de port si **8090** est déjà pris : `netstat -ano | findstr :8090`.

- Swagger : `http://localhost:8090/docs`
- Santé : `http://localhost:8090/health` ou (même chemin que via Gateway) `http://localhost:8090/api/evaluation-reports/health`

### Démarrage rapide (Windows)

Depuis ce dossier :

```powershell
.\run.ps1
```

### Si `pip install` échoue encore (Pillow)

Mettre à jour pip puis réessayer : `python -m pip install --upgrade pip`  
Ou installer Pillow seul : `pip install "pillow>=10.4,<12"`

### Si `pydantic-core` compile longtemps ou échoue (Python 3.14)

Le fichier `requirements.txt` impose **pydantic ≥ 2.11** pour obtenir une **roue** `pydantic_core` (évite Rust).  
Si pip reste bloqué sur une vieille résolution :

```powershell
python -m pip install --upgrade pip
pip install "pydantic>=2.11,<3" "fastapi>=0.115.6,<0.116"
pip install -r requirements.txt
```

Pour forcer une installation **uniquement** binaire (échec clair si pas de roue) :

```powershell
pip install --only-binary pydantic-core "pydantic>=2.11,<3"
pip install -r requirements.txt
```

### Toujours utiliser `python -m uvicorn`

Même avec le venv activé, préférer **`python -m uvicorn app.main:app --reload --port 8090`** pour être sûr d’utiliser le Python du venv (évite « uvicorn introuvable »).

## Via la Gateway

Avec la Gateway sur `8050` :

- `POST http://localhost:8050/api/evaluation-reports/charts/histogram`
- `POST http://localhost:8050/api/evaluation-reports/charts/evolution`
- `POST http://localhost:8050/api/evaluation-reports/reports/pdf`

Variable d’environnement pour la Gateway : `GATEWAY_EVALUATION_REPORTS_URL` (ex. `http://evaluation-reports:8090` sous Docker).

## Docker

```powershell
docker build -t plat-evaluation-reports:local .
docker run -p 8090:8090 plat-evaluation-reports:local
```

## Exemples JSON

**Histogramme** — `POST .../charts/histogram` :

```json
{ "scores": [12, 14, 15, 11, 16, 18, 13], "title": "Notes module Java" }
```

**Évolution** — `POST .../charts/evolution` :

```json
{ "labels": ["S1", "S2", "S3", "S4"], "values": [11.2, 12.5, 13.0, 14.2], "title": "Moyenne promo" }
```

**PDF** — `POST .../reports/pdf` :

```json
{
  "title": "Rapport d'évaluation",
  "subtitle": "Export entreprise",
  "scores": [10, 12, 14, 15, 16, 12, 13],
  "evolution_labels": ["Jan", "Fév", "Mar"],
  "evolution_values": [11.0, 12.5, 13.8]
}
```

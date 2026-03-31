#!/usr/bin/env python3
"""
Lance le service Team AI (analyse projet + construction d'équipe).
Usage : python run.py   ou   uvicorn app.main:app --host 0.0.0.0 --port 5000
"""
import uvicorn
from app.config import get_settings

if __name__ == "__main__":
    s = get_settings()
    uvicorn.run(
        "app.main:app",
        host=s.host,
        port=s.port,
        reload=True,
    )

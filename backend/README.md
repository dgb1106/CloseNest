# CloseNest FastAPI Backend

Development API for the MVP relationship flow.

## Run

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Relationship Endpoints

```http
GET    /relationships
POST   /relationships
GET    /relationships/{id}
PUT    /relationships/{id}
DELETE /relationships/{id}
```

This service currently uses an in-memory store so the Android relationship UI can be developed safely before PostgreSQL, JWT, and Retrofit sync are wired in.

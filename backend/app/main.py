from datetime import date, datetime, timezone
from enum import Enum
from typing import Optional
from uuid import uuid4

from fastapi import FastAPI, HTTPException, Response, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


app = FastAPI(title="CloseNest API", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


class RelationshipTag(str, Enum):
    family = "family"
    friend = "friend"
    close_friend = "close_friend"
    classmate = "classmate"
    coworker = "coworker"
    mentor = "mentor"
    partner = "partner"
    other = "other"


class InteractionType(str, Enum):
    meet = "meet"
    chat = "chat"
    call = "call"
    message = "message"
    gift = "gift"
    date = "date"
    other = "other"


class RelationshipBase(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    tag: RelationshipTag
    birthday: Optional[date] = None
    phone_number: Optional[str] = Field(default=None, max_length=32)
    email: Optional[str] = Field(default=None, max_length=160)
    interests: list[str] = Field(default_factory=list)
    notes: Optional[str] = Field(default=None, max_length=2_000)
    avatar_url: Optional[str] = None
    priority: int = Field(default=2, ge=1, le=3)


class RelationshipCreate(RelationshipBase):
    pass


class RelationshipUpdate(BaseModel):
    name: Optional[str] = Field(default=None, min_length=1, max_length=120)
    tag: Optional[RelationshipTag] = None
    birthday: Optional[date] = None
    phone_number: Optional[str] = Field(default=None, max_length=32)
    email: Optional[str] = Field(default=None, max_length=160)
    interests: Optional[list[str]] = None
    notes: Optional[str] = Field(default=None, max_length=2_000)
    avatar_url: Optional[str] = None
    priority: Optional[int] = Field(default=None, ge=1, le=3)


class RelationshipRead(RelationshipBase):
    id: str
    user_id: str
    last_interaction_type: Optional[InteractionType] = None
    last_interaction_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime


DEMO_USER_ID = "demo-user"
relationships: dict[str, RelationshipRead] = {}


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def seed_relationships() -> None:
    if relationships:
        return

    for item in [
        RelationshipCreate(
            name="Minh Anh",
            tag=RelationshipTag.close_friend,
            birthday=date(2003, 11, 12),
            phone_number="0901234567",
            email="minhanh@example.com",
            interests=["Ca phe", "Anh film"],
            notes="Hay di bo buoi toi va thich nhung loi nhan ngan gon.",
            priority=3,
        ),
        RelationshipCreate(
            name="Gia Han",
            tag=RelationshipTag.family,
            birthday=date(2000, 6, 21),
            phone_number="0988123456",
            interests=["Bep nuc", "Du lich"],
            notes="Thuong ranh sau 20h, hop de goi dien cuoi tuan.",
            priority=3,
        ),
    ]:
        create_relationship(item)


def create_relationship(payload: RelationshipCreate) -> RelationshipRead:
    now = utc_now()
    relationship = RelationshipRead(
        id=str(uuid4()),
        user_id=DEMO_USER_ID,
        created_at=now,
        updated_at=now,
        **payload.model_dump(),
    )
    relationships[relationship.id] = relationship
    return relationship


@app.on_event("startup")
def on_startup() -> None:
    seed_relationships()


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/relationships", response_model=list[RelationshipRead])
def list_relationships() -> list[RelationshipRead]:
    return sorted(relationships.values(), key=lambda item: item.name.lower())


@app.post(
    "/relationships",
    response_model=RelationshipRead,
    status_code=status.HTTP_201_CREATED,
)
def add_relationship(payload: RelationshipCreate) -> RelationshipRead:
    return create_relationship(payload)


@app.get("/relationships/{relationship_id}", response_model=RelationshipRead)
def get_relationship(relationship_id: str) -> RelationshipRead:
    relationship = relationships.get(relationship_id)
    if relationship is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Relationship not found")
    return relationship


@app.put("/relationships/{relationship_id}", response_model=RelationshipRead)
def update_relationship(relationship_id: str, payload: RelationshipUpdate) -> RelationshipRead:
    current = relationships.get(relationship_id)
    if current is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Relationship not found")

    updates = payload.model_dump(exclude_unset=True)
    updated = current.model_copy(update={**updates, "updated_at": utc_now()})
    relationships[relationship_id] = updated
    return updated


@app.delete("/relationships/{relationship_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_relationship(relationship_id: str) -> Response:
    if relationships.pop(relationship_id, None) is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Relationship not found")
    return Response(status_code=status.HTTP_204_NO_CONTENT)

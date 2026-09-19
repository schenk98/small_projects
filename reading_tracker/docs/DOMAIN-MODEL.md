# Doménový model

## Entity (MVP)

### User

- id, email (unique), passwordHash, role (STUDENT | TEACHER)
- avatarPresetId (nullable)
- blockedUntil (nullable, teacher)

### SchoolClass

- id, name (např. „7.A“)

### ClassMembership

- userId, schoolClassId, assignedAt, assignedByUserId

### Book

- id, ownerUserId, title, author, totalPages, status (READING | FINISHED), lastPage

### ReadingSession

- id, bookId, sessionDate, minutes, pagesFrom, pagesTo, note, pointsAwarded, markedFinished, createdAt

### QuizQuestion

- id, text, defaultPoints, active

### QuizResponse

- id, readingSessionId, quizQuestionId, answerText, pointsAwarded

### ShopItem

- id, name, description, pricePoints, cooldownDays (nullable), active

### Purchase

- id, userId, shopItemId, pricePoints (snapshot), purchasedAt

### LedgerAdjustment

- id, userId, amount, reason, createdByUserId, createdAt

### AppSetting

- key (PK), value (string; parse v service)

### FeaturedPost

- id, text, imageBlobId (nullable), linkUrl (nullable), sortOrder, active

### ImageBlob

- id, mimeType, data (bytea)

### AvatarPreset

- id, name, imageBlobId, active

### AuditEvent

- id, actorUserId, action, entityType, entityId, payloadJson, createdAt

## Vztahy

- User 1—* Book
- Book 1—* ReadingSession
- ReadingSession 0—1 QuizResponse
- User *—* SchoolClass via ClassMembership
- User 1—* Purchase, LedgerAdjustment

## Flyway

Migrace v `src/main/resources/db/migration/` — první verze v MVP-01.

# --- !Ups

ALTER TABLE histories modify COLUMN BorrowDate Timestamp NULL DEFAULT NULL;
ALTER TABLE histories modify COLUMN ReturnDate Timestamp NULL DEFAULT NULL;
ALTER TABLE histories modify COLUMN CreatedAt Timestamp NULL DEFAULT NULL;
ALTER TABLE histories modify COLUMN UpdatedAt Timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;

# --- !Downs

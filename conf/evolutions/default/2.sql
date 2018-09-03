# --- !Ups
INSERT INTO bike_status(ID, NAME) VALUES (1, 'Available');
INSERT INTO bike_status(ID, NAME) VALUES (2, 'Borrowed');
INSERT INTO bike_status(ID, NAME) VALUES (3, 'Out of order');

# --- !Downs
DELETE FROM bike_status WHERE NAME='Available';
DELETE FROM bike_status WHERE NAME='Borrowed';
DELETE FROM bike_status WHERE NAME='Out of order';

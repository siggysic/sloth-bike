# --- !Ups
INSERT INTO bike_status(NAME) VALUES ('Available');
INSERT INTO bike_status(NAME) VALUES ('Borrowed');
INSERT INTO bike_status(NAME) VALUES ('OutOfOrder');

# --- !Downs
DELETE FROM bike_status WHERE NAME='Available';
DELETE FROM bike_status WHERE NAME='Borrowed';
DELETE FROM bike_status WHERE NAME='OutOfOrder';

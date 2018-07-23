# --- !Ups

CREATE TABLE stations (
    Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL, Location VARCHAR(255));

CREATE TABLE bike_status (
    Id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    UNIQUE(Name));

CREATE TABLE bikes (
    Id VARCHAR(255) NOT NULL PRIMARY KEY,
    PieceNo VARCHAR(255),
    KeyBarcode VARCHAR(255),
    ReferenceId VARCHAR(255),
    LicensePlate VARCHAR(255),
    LotNo VARCHAR(255),
    Remark VARCHAR(255),
    Detail VARCHAR(255),
    ArrivalDate TIMESTAMP NOT NULL,
    CreatedAt TIMESTAMP,
    UpdatedAt TIMESTAMP,
    StatusId INT NOT NULL,
    StationId INT NOT NULL,
    INDEX (StatusId),
    INDEX (StationId),
    FOREIGN KEY (StatusId)
      REFERENCES bike_status(Id)
      ON UPDATE CASCADE,
    FOREIGN KEY (StationId)
      REFERENCES stations(Id)
      ON UPDATE CASCADE
    );

CREATE TABLE students (
    Id VARCHAR(12) NOT NULL PRIMARY KEY,
    FirstName VARCHAR(255),
    LastName VARCHAR(255),
    Major VARCHAR(255),
    ProfilePicture VARCHAR(255)
);

CREATE TABLE payments (
    Id VARCHAR(255) NOT NULL PRIMARY KEY,
    OvertimeFine INT,
    DefectFine INT,
    Note VARCHAR(255),
    CreatedAt TIMESTAMP,
    UpdatedAt TIMESTAMP,
    ParentId VARCHAR(255),
    INDEX (ParentId),
    FOREIGN KEY (ParentId)
      REFERENCES payments(Id)
      ON UPDATE CASCADE
      ON DELETE CASCADE
    );

CREATE TABLE histories (
    Id VARCHAR(255) NOT NULL PRIMARY KEY,
    StudentId VARCHAR(255) NOT NULL,
    Remark VARCHAR(255),
    BorrowDate TIMESTAMP,
    ReturnDate TIMESTAMP,
    CreatedAt TIMESTAMP,
    UpdatedAt TIMESTAMP,
    StationId INT,
    StatusId INT NOT NULL,
    BikeId VARCHAR(255) NOT NULL,
    PaymentId VARCHAR(255),
    INDEX (StatusId),
    INDEX (BikeId),
    INDEX (PaymentId),
    FOREIGN KEY (StatusId)
      REFERENCES bike_status(Id)
      ON UPDATE CASCADE,
    FOREIGN KEY (StationId)
      REFERENCES stations(Id)
      ON UPDATE CASCADE,
    FOREIGN KEY (BikeId)
      REFERENCES bikes(Id)
      ON UPDATE CASCADE,
    FOREIGN KEY (PaymentId)
      REFERENCES payments(Id)
      ON UPDATE CASCADE
    );


# --- !Downs

DROP TABLE histories
DROP TABLE payments
DROP TABLE students
DROP TABLE bikes
DROP TABLE bike_status
DROP TABLE stations
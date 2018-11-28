# --- !Ups
INSERT INTO Authentications (Id, Username, Password, Status, Role)
VALUES ('user:1',	'test', '$2a$10$E2i5i.kIsGv3jPkwN4038e/886yAaUy09okaqhRBGLC.W1P/k5aui', 'active', 'admin'),
('user:2',	'admin', '$2a$10$dAzcwWoLVQCF/XbTvwgPj.N28gU2WfrwDP7mQ2V6jmMghmJGnDzEW', 'active', 'admin'),
('user:3',	'webadmin', '$2a$10$CaojdHNciTDuyqL60q.Nl.IkDTC2P9IB4ZSLfOZ0z.tw6N6fQpSmy', 'active', 'admin');

# --- !Downs
DELETE FROM Authentications;
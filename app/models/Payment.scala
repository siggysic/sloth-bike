package models

import java.sql.Timestamp
import java.util.UUID

case class Payment(id: UUID, overtimeFine: Option[Int], defectFine: Option[Int],
                   note: String,
                   createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   parentId: Option[UUID])

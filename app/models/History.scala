package models

import java.sql.Timestamp
import java.util.UUID


case class History(id: UUID, studentId: String, remark: Option[String],
                   borrowDate: Timestamp, returnDate: Option[Timestamp],
                   createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   statusId: Int, bikeId: String, paymentId: Option[UUID])
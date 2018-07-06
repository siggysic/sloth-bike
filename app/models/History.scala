package models

import java.sql.Timestamp
import java.util.UUID


case class History(id: UUID, studentId: String, remark: Option[String],
                   borrowDate: Timestamp, returnDate: Option[Timestamp],
                   createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   station: Option[Int], statusId: Int, bikeId: String, paymentId: Option[UUID])

case class HistoryQuery(lotNo: Option[String], station: Option[String], returnFrom: Option[Timestamp], returnTo: Option[Timestamp], page: Int, pageSize: Int)
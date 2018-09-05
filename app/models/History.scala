package models

import java.sql.Timestamp
import java.util.UUID


case class History(id: String, studentId: Option[String], remark: Option[String],
                   borrowDate: Option[Timestamp], returnDate: Option[Timestamp],
                   createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   station: Option[Int], statusId: Int, bikeId: String, paymentId: Option[String])

case class HistoryQuery(bikeId: Option[String], lotNo: Option[String], station: Option[String], returnFrom: Option[Timestamp], returnTo: Option[Timestamp], page: Int, pageSize: Int)

case class HistoryWithPayment(history: History, payment: Payment, student: Student)

case class HistoryWithStatus(id: String, studentId: Option[String], remark: Option[String],
                             borrowDate: Option[Timestamp], returnDate: Option[Timestamp],
                             status: BikeStatus)

case class FineResult(historyId: String, fine: Int, isStudentBorrow: Boolean)

package models

import java.sql.Timestamp

case class Payment(id: String, overtimeFine: Option[Int], defectFine: Option[Int],
                   note: Option[String] = None,
                   createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                   parentId: Option[String] = None)

case class PaymentSummary(id: String, overtimeFine: Option[Int], defectFine: Option[Int])


case class FullPayment(paymentId: String, historyId: String, studentId: String, firstName: String, lastName: String,
                       major: String, overtimeFine: Option[Int], defectFine: Option[Int])
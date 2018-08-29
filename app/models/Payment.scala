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

case class PaymentReturn(overtimeFine: Int, defectFine: Int, note: String)

case class PaymentResp(id: String, overtimeFine: Int, defectFine: Int, note: String)
case class ModalResponse(student: Student, payment: PaymentResp, history: History)

case class PaymentReq(fine: Option[Int],
                       note: Option[String] = None,
                       parentId: Option[String] = None)


case class PaymentQuery(studentId: Option[String], firstName: Option[String], lastName: Option[String], major: Option[String], page: Int, pageSize: Int = 10)
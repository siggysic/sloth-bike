package models

import java.sql.Timestamp
import java.util.UUID

case class History(id: UUID, fine: Int, remark: String,
                   borrowDate: Timestamp, returnDate: Timestamp,
                   bikeId: UUID, statusId: Int)


case class payment(studentId: String, returnStatus: String, fine1: Int, fine2: Int, note: String)

case class
package models

import java.sql.Timestamp
import java.util.UUID

case class Bike(id: String, keyBarcode: Option[String], referenceId: String, lotNo: String,
                licensePlate: String, remark: Option[String], detail: Option[String],
                arrivalDate: Timestamp, pieceNo: String,
                createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                statusId: Int, stationId: Int) {
  def toBikeRequest = BikeRequestWithField(
    id = Some(id), keyBarcode = this.keyBarcode.getOrElse(""), referenceId = this.referenceId, lotNo = this.lotNo,
    licensePlate = this.licensePlate, detail = this.detail, arrivalDate = new java.util.Date(this.arrivalDate.getTime),
    pieceNo = this.pieceNo, statusId = this.statusId, stationId = this.stationId, field = None
  )
}

case class BikeQuery(statusId: Option[Int], page: Int, pageSize: Int)

case class BikeRequest(
                        id: Option[String], keyBarcode: String, referenceId: String, lotNo: String,
                        licensePlate: String, detail: Option[String], arrivalDate: java.util.Date,
                        pieceNo: String, statusId: Int, stationId: Int
                      ) {
  def toBike = Bike(
    id = this.id.getOrElse(java.util.UUID.randomUUID.toString), keyBarcode = Some(this.keyBarcode), referenceId = this.referenceId,
    lotNo = this.lotNo, licensePlate = this.licensePlate, remark = None, detail = this.detail,
    arrivalDate = new Timestamp(this.arrivalDate.getTime), pieceNo = this.pieceNo,
    statusId = statusId, stationId = stationId
  )
}

case class BikeRequestWithField(
                        id: Option[String], keyBarcode: String, referenceId: String, lotNo: String,
                        licensePlate: String, detail: Option[String], arrivalDate: java.util.Date,
                        pieceNo: String, statusId: Int, stationId: Int, field: Option[String]
                      ) {
  def toBike = Bike(
    id = this.id.getOrElse(java.util.UUID.randomUUID.toString), keyBarcode = Some(this.keyBarcode), referenceId = this.referenceId,
    lotNo = this.lotNo, licensePlate = this.licensePlate, remark = None, detail = this.detail,
    arrivalDate = new Timestamp(this.arrivalDate.getTime), pieceNo = this.pieceNo,
    statusId = statusId, stationId = stationId
  )
}

case class BikeImport(
                              lotNo: String,
                              detail: Option[String],
                              statusId: Int,
                              stationId: Int
                            )

case class BikeFields(
                       arrivalDate: String = "arrivalDate",
                       lotNo: String = "lotNo",
                       detail: String = "detail",
                       pieceNo: String = "pieceNo",
                       licensePlate: String = "licensePlate",
                       keyBarcode: String = "keyBarcode",
                       referenceId: String = "referenceId",
                       statusId: String = "statusId",
                       stationId: String = "stationId",
                       field: String = "field"
                     )

case class BikeSearch(
                       lotNo: Option[String] = None,
                       pieceNo: Option[String] = None,
                       licensePlate: Option[String] = None,
                       keyBarcode: Option[String] = None,
                       referenceId: Option[String] = None,
                       statusId: Option[Int] = None
                     )

case class BikeTotal(
                    total: Int
                    )

case class BikeReturnReq(keyBarcode: String, historyId: String, payment: Option[PaymentReturn])
case class BikeRepairReturnReq(keyBarcode: String)

case class BikeBorrowedReq(studentId: String, keyBarcode: String)
case class BikeRepairReq(keyBarcode: String)

case class BikeReturn(bike: Bike, student: Student, history: History, overtimeDate: String, overtimePayment: Long)
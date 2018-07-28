package models

import java.sql.Timestamp
import java.util.UUID

case class Bike(id: String, keyBarcode: Option[String], referenceId: String, lotNo: String,
                licensePlate: String, remark: Option[String], detail: Option[String],
                arrivalDate: Timestamp, pieceNo: String,
                createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                statusId: Int, stationId: Int)

case class BikeQuery(statusId: Option[Int], page: Int, pageSize: Int)

case class BikeRequest(
                        keyBarcode: String, referenceId: String, lotNo: String,
                        licensePlate: String, detail: Option[String], arrivalDate: java.util.Date,
                        pieceNo: String, statusId: Int, stationId: Int
                      ) {
  def toBike = Bike(
    id = java.util.UUID.randomUUID().toString, keyBarcode = Some(this.keyBarcode), referenceId = this.referenceId,
    lotNo = this.lotNo, licensePlate = this.licensePlate, remark = None, detail = this.detail,
    arrivalDate = new Timestamp(this.arrivalDate.getTime), pieceNo = this.pieceNo,
    statusId = statusId, stationId = stationId
  )
}

case class BikeFields(
                       arrivalDate: String = "arrivalDate",
                       lotNo: String = "lotNo",
                       detail: String = "detail",
                       pieceNo: String = "pieceNo",
                       licensePlate: String = "licensePlate",
                       keyBarcode: String = "keyBarcode",
                       referenceId: String = "referenceId",
                       statusId: String = "statusId",
                       stationId: String = "stationId"
                     )

case class BikeSearch(
                       lotNo: Option[String],
                       pieceNo: Option[String],
                       licensePlate: Option[String],
                       keyBarcode: Option[String],
                       referenceId: Option[String],
                       statusId: Option[Int]
                     )


case class BikeReturnReq(keyBarcode: String, historyId: String, paymentReq: PaymentReturn)
case class BikeRepairReturnReq(keyBarcode: String)

case class BikeBorrowedReq(keyBarcode: String)
case class BikeRepairReq(keyBarcode: String)
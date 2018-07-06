package models

import java.sql.Timestamp

case class Bike(id: String, keyBarcode: Option[String], referenceId: String, lotNo: String,
                licensePlate: String, remark: Option[String], detail: Option[String],
                createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()),
                statusId: Int)

case class BikeQuery(statusId: Option[Int], page: Int, pageSize: Int)
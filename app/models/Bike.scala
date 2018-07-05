package models

import java.sql.Timestamp

case class Bike(id: String, keyBarcode: String, referenceId: String,
                licensePlate: String, remark: String, detail: String,
                createdAt: Timestamp, updatedAt: Timestamp, statusId: Int)

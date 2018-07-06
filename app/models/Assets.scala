package models

case class Asset (
                   date: java.util.Date,
                   slot_number: String,
                   detail: String,
                   number_of_pieces: String,
                   license_plate: String,
                   key_barcode: String,
                   rfid: String,
                   status: String,
                   station: String
                 )

case class AssetFields(
                       date: String = "date",
                       slot_number: String = "slot_number",
                       detail: String = "detail",
                       number_of_pieces: String = "number_of_pieces",
                       license_plate: String = "license_plate",
                       key_barcode: String = "key_barcode",
                       rfid: String = "rfid",
                       status: String = "status",
                       station: String = "station"
                     )
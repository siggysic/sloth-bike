package models

case class Student(id: String, firstName: String, lastName: String,
                   phone: String, major: Option[String], `type`: String, status: String,
                   address: Option[String], department: Option[String],
                   profilePicture: Option[String])

case class StudentResponse(student: Student)

case class StudentFields(
                          id: String = "id", firstName: String = "firstName", lastName: String = "lastName",
                          phone: String = "phone", major: String = "major", `type`: String = "type", status: String = "status",
                          address: String = "address", department: String = "department",
                          profilePicture: String = "profilePicture"
                        )
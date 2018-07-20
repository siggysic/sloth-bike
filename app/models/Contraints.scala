package models

import java.util.Date

import play.api.data.{Forms, Mapping}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object Contraints {

  def validateDate: Constraint[Date] = Constraint[Date]("constraint.required") { v =>
    if (v == null) Invalid(ValidationError("This field is required"))
    else if(new Date().after(v)) Invalid(ValidationError("Invalid date"))
    else Valid
  }

  def validateText: Constraint[String] = Constraint[String]("constraint.required") { v =>
    if (v == null) Invalid(ValidationError("This field is required"))
    else if (v.trim.isEmpty) Invalid(ValidationError("This field is required"))
    else Valid
  }
}
package controllers

import javax.inject.{Inject, _}
import models.{Asset, AssetFields, Contraints}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

@Singleton
class AssetsController @Inject()(cc: ControllerComponents) (implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  val form: Form[Asset] = Form (
    mapping(
      "date" -> date.verifying(Contraints.validateDate),
      "slot_number" -> text.verifying(Contraints.validateText),
      "detail" -> text,
      "number_of_pieces" -> text.verifying(Contraints.validateText),
      "license_plate" -> text.verifying(Contraints.validateText),
      "key_barcode" -> text.verifying(Contraints.validateText),
      "rfid" -> text.verifying(Contraints.validateText),
      "station" -> text.verifying(Contraints.validateText)
    )(Asset.apply)(Asset.unapply)
  )

  val fields = AssetFields()

  def viewInsertAssets = Action {
    Ok(views.html.assetsInsert(form, fields))
  }

  def insertAssets = Action { implicit request: Request[AnyContent] =>

    val errorFunction = { formWithErrors: Form[Asset] =>
      BadRequest(views.html.assetsInsert(formWithErrors, fields))
    }

    val successFunction = { data: Asset =>
      Ok(views.html.assetsInsert(form, fields))
    }


    val formValidationResult: Form[Asset] = form.bindFromRequest
    formValidationResult.fold(
      errorFunction,
      successFunction
    )

//    Ok(views.html.assetsInsert(form))
  }

}
package webby.form
import java.util.Locale

import querio._
import webby.commons.io.StdJs
import webby.form.field.Field
import webby.form.i18n.FormStrings
import webby.html._
import webby.html.elements.RichSelectConfig

abstract class BaseForms {self =>
  def db: DbTrait

  def strings(locale: Locale): FormStrings

  def maybeChangedFieldsDao: Option[ChangedFieldsDao] = None

  def js = StdJs.get

  /** @see [[webby.form.Form.jsConfig]] */
  def jsConfig: String = null

  def formId = "form"
  def subFormId = "$subform$"

  // ------------------------------- Form traits -------------------------------

  trait BaseCommon extends Form {
    override type B = self.type
    override def base: B = self
    override def initLocale: Locale = Locale.ENGLISH
    override def jsConfig: String = self.jsConfig
  }

  trait BaseWithDb[PK, TR <: TableRecord[PK], MTR <: MutableTableRecord[PK, TR]] extends FormWithDb[PK, TR, MTR] with BaseCommon

  // ------------------------------- Html helpers -------------------------------

  def formCls = "form"
  def hiddenCls = "hidden"
  def hideFormByDefault = true

  def formGroupCls = "form-group"
  def formErrorsBlockCls = "form-errors-block"
  def formRowCls = "form-row"

  def fieldCls = "field"
  def fieldLabelCls = "field-label"

  def checkboxLeftCls = "checkbox-left"
  def autocompleteListFieldCls = "autocomplete-list-field"
  def autocompleteListItemsCls = "autocomplete-list__items"
  def dateFieldCls = "date-field"
  def monthYearFieldCls = "month-year-field"

  def formCreateInitJsCode(form: Form): String = "Form.createInit(" + js.toJson(form.jsProps) + ")"

  def formTag(scripts: JsCodeAppender, form: Form, id: String, method: String)(implicit view: HtmlBase): StdFormTag = {
    scripts.addCode(formCreateInitJsCode(form))
    view.form.cls(formCls).clsIf(hideFormByDefault, hiddenCls).id(id).method(method)
  }

  def makeFieldHtmlId(field: Field[_]): String = field.form.htmlId + "-" + field.shortId

  def selectConfig = new RichSelectConfig()
}


trait ChangedItemType


trait ChangedFieldsDao {
  case class ChangedFieldValue(fieldTitle: String, path: String, value: String)

  def query(tpe: ChangedItemType, item: TableRecord[_]): Vector[ChangedFieldValue]

  // ------------------------------- Modification methods -------------------------------

  def insert(tpe: ChangedItemType, itemId: Any, field: AnyTable#ThisField, id: Any)(implicit tr: Transaction)

  def delete(tpe: ChangedItemType, itemId: Any)(implicit tr: Transaction): Unit
}

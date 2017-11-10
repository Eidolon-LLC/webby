package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import webby.commons.text.validator.UrlValidator
import webby.form.{Form, Invalid, Valid, ValidationResult}

class UrlField(val form: Form,
               val shortId: String,
               allowedDomains: Vector[String] = Vector.empty)
  extends ValueField[String] with PlaceholderField[String] {self =>

  var allowedSchemes: Seq[String] = Vector("http", "https")
  var defaultScheme: String = "http"

  val MaxLength = 250

  // ------------------------------- Reading data & js properties -------------------------------
  override def jsField: String = "text"
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node)(Right(_))
  override def nullValue: String = null

  /** Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue. */
  override protected def convertValue(v: String): String = {
    if (v != null && !v.contains("://")) defaultScheme + "://" + v
    else v
  }

  // ------------------------------- Builder & validations -------------------------------

  def allowedSchemes(v: Seq[String]): this.type = {allowedSchemes = v; this}
  def defaultScheme(v: String): this.type = {defaultScheme = v; this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (get.length > MaxLength) Invalid(form.strings.noMoreThanCharsError(MaxLength))
    else UrlValidator.validate(get, allowedSchemes = allowedSchemes) match {
      case None => Invalid(form.strings.invalidUrl)
      case Some(url) =>
        if (allowedDomains.nonEmpty && !UrlValidator.validateDomain(url, allowedDomains)) {
          if (allowedDomains.size == 1) return Invalid(form.strings.urlMustContainDomain(allowedDomains.head))
          else return Invalid(form.strings.urlMustContainOneOfDomains(allowedDomains.mkString(", ")))
        }
        Valid
    }
  }
}

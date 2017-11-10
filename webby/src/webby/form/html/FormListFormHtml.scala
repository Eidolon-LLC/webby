package webby.form.html
import webby.form.field.FormListField
import webby.form.{Form, SubForm}
import webby.html.{CommonTag, HtmlBase}

trait FormListFormHtml {self: StdFormHtml =>

  // ------------------------------- CSS styles -------------------------------

  /** id для html элемента удаления записи */
  def formListRemoveId: String = "form__remove"

  def formSubFormCls = "form__subform"

  def formSmallAddCls = "form__small-add"
  def formSmallRemoveCls = "form__small-remove"
  def formBlockAddCls = "form__block-add"
  def formBlockRemoveCls = "form__block-remove"

  // ------------------------------- Html methods -------------------------------

  /**
    * Шаблон подформы, который клонируется при добавлении нового элемента.
    *
    * @param tag   Обрамляющий тег шаблона. Клонируется именно этот тег.
    * @param tagFn Дополнительные действия с обрамляющим тегом
    * @param body  Тело шаблона
    */
  def formListTemplate[F <: SubForm](field: FormListField[F], tag: String = "div", tagFn: CommonTag => CommonTag = identity)(body: F => Any): HtmlBase =
    tagFn(view.tag(tag).id(field.htmlTemplateId).cls(form.base.hiddenCls)) < body(field.formStub)

  def formListRowTemplate[F <: SubForm](field: FormListField[F], tag: String = "div", tagFn: CommonTag => CommonTag = identity)(body: F => Any): HtmlBase =
    formListTemplate(field, tag, t => tagFn(t.cls(form.base.formRowCls)))(body)

  def formListBlockTemplate[F <: SubForm](field: FormListField[F], tagFn: CommonTag => CommonTag = identity)(body: F => Any): HtmlBase =
    tagFn(group.id(field.htmlTemplateId).cls(form.base.hiddenCls).cls(formSubFormCls)) {
      formListBlockRemoveTag(field)
      body(field.formStub)
    }

  def formListPlaceholder(field: FormListField[_ <: Form]): CommonTag = view.div.id(field.htmlListId)
  def formListSmallAddTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls(formSmallAddCls).id(field.htmlAddId)
  def formListSmallRemoveTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls(formSmallRemoveCls).id(formListRemoveId)
  def formListBlockAddTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls(formBlockAddCls).id(field.htmlAddId)
  def formListBlockRemoveTag(field: FormListField[_ <: Form]): CommonTag = view.a.hrefAnchor.cls(formBlockRemoveCls).id(formListRemoveId)
}

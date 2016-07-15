package webby.mvc.script

import webby.commons.io.Resources

/**
  * Трейт, позволяющий контроллеру хранить js-код с таким же именем, как и контроллер, который добавляется в тело самого контроллера.
  *
  * Для использования:
  * 1. Контроллер должен наследовать JsClassHolder
  * 2. Подключить на страницу: page.addClassJsToScripts()
  */
trait JsClassHolder {
  implicit protected def _jsClassHolder: JsClassHolder = this

  val jsResHolder = StdResourceHolder.jsMin(Resources.nameForClass(getClass, ".js"))
}

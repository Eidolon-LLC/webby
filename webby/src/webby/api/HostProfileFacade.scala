package webby.api
import javax.annotation.Nullable

/**
  * Заглушка и немного классов с методов, чтобы подключиться к реализации HostProfile в проекте.
  * HostProfile нужен для разделения реальных физических серверов. Он содержит информацию о том,
  * на каком сейчас сервере крутится приложение. В продакшне он задаётся один раз при старте,
  * и не может быть изменён. В dev/jenkins режиме, он задаётся для каждого запроса отдельно
  * (через [[ThreadLocal]]).
  *
  * Пример реализации:
  * {{{
  * trait HostProfile extends StdHostProfile {
  *   // Запрос обрабатывается на основном сервере Росработы?
  *   def isRosrabota: Boolean
  *
  *   // Запрос обрабатывается на сервере Ио?
  *   def isIo: Boolean
  * }
  *
  * object RosrabotaHostProfile extends HostProfile {
  *   override def isRosrabota: Boolean = true
  *   override def isIo: Boolean = false
  * }
  *
  * object IoHostProfile extends HostProfile {
  *   override def isRosrabota: Boolean = false
  *   override def isIo: Boolean = true
  * }
  *
  *
  * object HostProfile extends HostProfileObject[HostProfile] {
  *   override protected def profileByName: PartialFunction[String, HostProfile] = {
  *     case "rosrabota" => RosrabotaHostProfile
  *     case "io" => IoHostProfile
  *   }
  * }
  * }}}
  */
trait StdHostProfile

abstract class HostProfileObject[HP <: StdHostProfile] {
  /**
    * Профайл, заданный при старте приложения. Задаётся только в продакшне.
    * На локальной версии всегда null.
    */
  @Nullable val prodOrConsoleProfile: HP = init()

  @Nullable protected def init(): HP = {
    if (App.isProdOrConsole) {
      val conf = App.app.configuration
      profileByName.applyOrElse[String, HP](conf.getString("production-host").get, p => sys.error("Unknown HostProfile: " + p))
    } else null.asInstanceOf[HP]
  }

  protected def profileByName: PartialFunction[String, HP]

  protected val localProfile: ThreadLocal[HP] = new ThreadLocal[HP]

  @Nullable def get: HP = if (prodOrConsoleProfile != null) prodOrConsoleProfile else localProfile.get()

  def localSet(profile: HP): Unit = {
    checkNotProd()
    localProfile.set(profile)
  }
  def localRemove(): Unit = {
    checkNotProd()
    localProfile.remove()
  }

  protected def checkNotProd() {
    require(!App.isProd, "Cannot set HostProfile in production")
  }
}

class HostProfileObjectStub extends HostProfileObject[StdHostProfile] {
  @Nullable override protected def init(): StdHostProfile = null
  override protected def profileByName: PartialFunction[String, StdHostProfile] = null
}


object HostProfileFacade {
  private[api] var _delegate: HostProfileObject[StdHostProfile] = _

  def delegate: HostProfileObject[StdHostProfile] = {
    if (_delegate == null) _delegate = new HostProfileObjectStub
    _delegate
  }

  @Nullable def get: StdHostProfile = delegate.get
  def localSet(profile: StdHostProfile): Unit = delegate.localSet(profile)
  def localRemove(): Unit = delegate.localRemove()
}

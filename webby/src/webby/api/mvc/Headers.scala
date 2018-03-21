package webby.api.mvc
import io.netty.handler.codec.http.HttpHeaders

/**
  * The HTTP headers set.
  */
class Headers(val http: HttpHeaders) {

  import scala.collection.JavaConverters._

  /**
    * Optionally returns the first header value associated with a key.
    */
  def get(key: CharSequence): Option[String] = Option(http.get(key))

  def get(key: CharSequence, default: String): String = {
    val v: String = http.get(key)
    if (v == null) default else v
  }

  /**
    * Retrieves the first header value which is associated with the given key.
    */
  def apply(key: CharSequence): String = {
    val v: String = http.get(key)
    if (v == null) scala.sys.error("Header doesn't exist") else v
  }

  /**
    * Retrieve all header values associated with the given key.
    */
  def getAll(key: CharSequence): Iterable[String] = http.getAll(key).asScala

  override def toString = http.toString
}

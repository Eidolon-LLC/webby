package orm.elasticsearch

import java.{util => ju}
import javax.annotation.Nullable

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.common.document.DocumentField
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField

/**
  * Источник данных, полученных от ElasticSearch'а.
  * Эти данные нужны для создания объекта EsClass.
  */
trait EsClassData {
  @Nullable def getId: String
  @Nullable def get(field: String): AnyRef
  @Nullable def getHighlightedField(field: String): HighlightField
  def isFieldHighlighted(field: String): Boolean
  def score: Option[Float]
}

class EsClassHitData(val hit: SearchHit) extends EsClassData {
  val map = hit.getSourceAsMap
  val hf = hit.getHighlightFields
  @Nullable override def getId: String = hit.getId
  @Nullable override def get(field: String): AnyRef = map.get(field)
  @Nullable override def getHighlightedField(field: String): HighlightField = hf.get(field)
  override def isFieldHighlighted(field: String): Boolean = hf.containsKey(field)
  override def score: Option[Float] = Some(hit.getScore)
}

class EsClassHitFieldsData(val hit: SearchHit) extends EsClassData {
  val fields: ju.Map[String, DocumentField] = hit.getFields
  val hf = hit.getHighlightFields
  @Nullable override def getId: String = hit.getId
  @Nullable override def get(field: String): AnyRef = fields.get(field).getValue
  @Nullable override def getHighlightedField(field: String): HighlightField = hf.get(field)
  override def isFieldHighlighted(field: String): Boolean = hf.containsKey(field)
  override def score: Option[Float] = Some(hit.getScore)
}

class EsClassGetData(val response: GetResponse) extends EsClassData {
  val map = response.getSourceAsMap
  @Nullable override def getId: String = response.getId
  @Nullable override def get(field: String): AnyRef = map.get(field)
  @Nullable override def getHighlightedField(field: String): HighlightField = null
  def isFieldHighlighted(field: String): Boolean = false
  override def score: Option[Float] = None
}

class EsClassMapData(val map: ju.Map[String, AnyRef]) extends EsClassData {
  @Nullable override def getId: String = map.get("_id").asInstanceOf[String]
  @Nullable override def get(field: String): AnyRef = map.get(field)
  @Nullable override def getHighlightedField(field: String): HighlightField = null
  override def isFieldHighlighted(field: String): Boolean = false
  override def score: Option[Float] = Option(map.get("_score").asInstanceOf[Float])
}

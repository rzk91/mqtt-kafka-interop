package com.rzk.utils.implicits

import java.util.Properties
import scala.jdk.CollectionConverters._

class RichProperty(private val prop: Properties) extends AnyVal {
  def toMap: Map[String, String] = prop.entrySet.asScala.map(kv => (kv.getKey.toString, kv.getValue.toString)).toMap
}

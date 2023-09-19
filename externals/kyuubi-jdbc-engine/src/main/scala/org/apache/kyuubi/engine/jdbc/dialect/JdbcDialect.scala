/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kyuubi.engine.jdbc.dialect

import java.sql.{Connection, Statement}
import java.util
import java.util.ServiceLoader

import scala.collection.JavaConverters._

import org.apache.kyuubi.{KyuubiException, Logging}
import org.apache.kyuubi.config.KyuubiConf
import org.apache.kyuubi.config.KyuubiConf.{ENGINE_JDBC_CONNECTION_URL, ENGINE_JDBC_SHORT_NAME}
import org.apache.kyuubi.engine.jdbc.schema.{RowSetHelper, SchemaHelper}
import org.apache.kyuubi.engine.jdbc.util.SupportServiceLoader
import org.apache.kyuubi.operation.Operation
import org.apache.kyuubi.session.Session

abstract class JdbcDialect extends SupportServiceLoader with Logging {

  def createStatement(connection: Connection, fetchSize: Int = 1000): Statement

  def getTypeInfoOperation(session: Session): Operation

  def getCatalogsOperation(session: Session): Operation

  def getSchemasOperation(session: Session): Operation

  def getTablesQuery(
      catalog: String,
      schema: String,
      tableName: String,
      tableTypes: util.List[String]): String

  def getTableTypesOperation(session: Session): Operation

  def getColumnsQuery(
      session: Session,
      catalogName: String,
      schemaName: String,
      tableName: String,
      columnName: String): String

  def getFunctionsOperation(session: Session): Operation

  def getPrimaryKeysOperation(session: Session): Operation

  def getCrossReferenceOperation(session: Session): Operation

  def getRowSetHelper(): RowSetHelper

  def getSchemaHelper(): SchemaHelper

}

object JdbcDialects extends Logging {

  def get(conf: KyuubiConf): JdbcDialect = {
    val shortName: String = conf.get(ENGINE_JDBC_SHORT_NAME).getOrElse {
      val url = conf.get(ENGINE_JDBC_CONNECTION_URL).get
      assert(url.length > 5 && url.substring(5).contains(":"))
      url.substring(5, url.indexOf(":", 5))
    }
    val serviceLoader =
      ServiceLoader.load(classOf[JdbcDialect], Thread.currentThread().getContextClassLoader)
    serviceLoader.asScala.filter(_.name().equalsIgnoreCase(shortName)).toList match {
      case Nil =>
        throw new KyuubiException(s"Don't find jdbc dialect implement for jdbc engine: $shortName.")
      case head :: Nil =>
        head
      case managers =>
        warn(s"Found multiple jdbc dialect implements for jdbc engine: $shortName")
        managers
          .filter(_.getClass.getCanonicalName.startsWith("org.apache.kyuubi"))
          .head
    }
  }
}

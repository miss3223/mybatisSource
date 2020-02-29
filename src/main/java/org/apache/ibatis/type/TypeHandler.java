/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public interface TypeHandler<T> {

    /**
     *
     * 使用typeHandler通过PreparedStatement对象进行设置SQL参数的时候使用的具体方法
     * Java Type ==> JDBC Type
     * @param ps 设置SQL参数
     * @param i 参数在SQL的下标
     * @param parameter 参数
     * @param jdbcType 数据库类型
     * @throws SQLException 异常类型
     */
  void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    /**
     * 从JDBC结果集中获取数据进行转换
     *
     * JDBC Type ==> Java Type
     * @param rs 结果集
     * @param columnName 列名
     * @return 数据库数据
     * @throws SQLException 异常
     */
  T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     *
     * @param rs 结果集
     * @param columnIndex 下标
     * @return 数据库数据
     * @throws SQLException 异常
     */
  T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 存储过程专用
     * @param cs 回调数据
     * @param columnIndex 下标
     * @return 数据库数据
     * @throws SQLException 异常
     */
  T getResult(CallableStatement cs, int columnIndex) throws SQLException;

}

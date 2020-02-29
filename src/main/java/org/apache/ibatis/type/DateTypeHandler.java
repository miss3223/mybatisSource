/**
 *    Copyright 2009-2017 the original author or authors.
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
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Clinton Begin
 *
 * Date 类型的 TypeHandler 实现类
 */
public class DateTypeHandler extends BaseTypeHandler<Date> {

  /**
   * 将不为空的Date类型的参数存储的到PreparedStatement对象中
   * @param ps PreparedStatement对象
   * @param i 参数的索引位置
   * @param parameter 参数value
   * @param jdbcType jdbc类型
   * @throws SQLException sql异常
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
      throws SQLException {
    // 将Date的参数转换为TimeStamp类型，然后保存到PreparedStatement对象中
    ps.setTimestamp(i, new Timestamp(parameter.getTime()));
}


  /**
   * 获取不为空的Date类型的参数
   * @param rs 结果集
   * @param columnName 元素名称
   * @return 返回的Date类型的值
   * @throws SQLException sql异常
   */
  @Override
  public Date getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    // 在查询到的结果集中获取到Timestamp类型的参数值
    Timestamp sqlTimestamp = rs.getTimestamp(columnName);
    // 如果获取到的sqlTimestamp不为空的话，就将此参数转化为Date类型返回
    if (sqlTimestamp != null) {
      return new Date(sqlTimestamp.getTime());
    }
    // 如果为空则返回null
    return null;
  }

  /**
   *
   * 获取不为空的Date类型的参数  处理过程与上面的方法相同
   * @param rs 结果集
   * @param columnIndex 元素的索引
   * @return 返回的Date类型的值
   * @throws SQLException sql异常
   */
  @Override
  public Date getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    Timestamp sqlTimestamp = rs.getTimestamp(columnIndex);
    if (sqlTimestamp != null) {
      return new Date(sqlTimestamp.getTime());
    }
    return null;
  }

  /**
   * 获取不为空的Date类型的参数 处理过程与上面的方法相同
   * 存储过程专用
   * @param cs 回调数据
   * @param columnIndex 元素的索引
   * @return 返回的Date类型的值
   * @throws SQLException sql异常
   */
  @Override
  public Date getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    Timestamp sqlTimestamp = cs.getTimestamp(columnIndex);
    if (sqlTimestamp != null) {
      return new Date(sqlTimestamp.getTime());
    }
    return null;
  }
}

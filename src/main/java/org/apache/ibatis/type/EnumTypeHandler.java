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

/**
 * @author Clinton Begin
 *
 * java.lang.Enum 和 java.util.String 的互相转换
 * 在数据库中没有Enum类型的，因此，将Java中枚举类型的数据存储到数据库中，有两种方式：
 * Enum.name <=> String
 * Enum.ordinal <=> int
 * 在本类中，是将Enum.name <=> String
 *
 */
public class EnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  /** 枚举类 */
  private final Class<E> type;

  public EnumTypeHandler(Class<E> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type argument cannot be null");
    }
    this.type = type;
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
    // 如果 jdbcType为null，则将枚举转化为string类型存储到PreparedStatement对象中
    if (jdbcType == null) {
      ps.setString(i, parameter.name());
    }
    // 如果jdbcType对象不null，则根据jdbcType类型转化设置到PreparedStatement对象中
    else {
      ps.setObject(i, parameter.name(), jdbcType.TYPE_CODE); // see r3589
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    // 获得string的值
    String s = rs.getString(columnName);
    // 将string值转化为枚举类型
    return s == null ? null : Enum.valueOf(type, s);
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String s = rs.getString(columnIndex);
    return s == null ? null : Enum.valueOf(type, s);
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String s = cs.getString(columnIndex);
    return s == null ? null : Enum.valueOf(type, s);
  }
}
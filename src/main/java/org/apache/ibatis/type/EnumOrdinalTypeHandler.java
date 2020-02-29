/**
 *    Copyright 2009-2018 the original author or authors.
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
 *  * java.lang.Enum 和 java.util.String 的互相转换
 *  * 在数据库中没有Enum类型的，因此，将Java中枚举类型的数据存储到数据库中，有两种方式：
 *  * Enum.name <=> String
 *  * Enum.ordinal <=> int
 *  * 在本类中，是将Enum.ordinal <=> int
 */
public class EnumOrdinalTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

  /** 枚举类 */
  private final Class<E> type;
  /** 枚举类型 */
  private final E[] enums;

  public EnumOrdinalTypeHandler(Class<E> type) {
    if (type == null) {
      throw new IllegalArgumentException("Type argument cannot be null");
    }
    this.type = type;
    this.enums = type.getEnumConstants();
    if (this.enums == null) {
      throw new IllegalArgumentException(type.getSimpleName() + " does not represent an enum type.");
    }
  }

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
    // 将Enum转化为int类型，保存到PreparedStatement对象中
    ps.setInt(i, parameter.ordinal());
  }

  @Override
  public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
    // 获取int值，将int转化为Enum类型
    int i = rs.getInt(columnName);
    if (i == 0 && rs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  @Override
  public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    int i = rs.getInt(columnIndex);
    if (i == 0 && rs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }

  @Override
  public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    int i = cs.getInt(columnIndex);
    if (i == 0 && cs.wasNull()) {
      return null;
    } else {
      try {
        return enums[i];
      } catch (Exception ex) {
        throw new IllegalArgumentException("Cannot convert " + i + " to " + type.getSimpleName() + " by ordinal value.", ex);
      }
    }
  }
  
}

package com.tujia.train.mybatis.typehandler;


import com.tujia.train.mybatis.core.ShortDate;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.text.ParseException;
import java.util.Date;

/**
 * @author jianhong.li Date: 15-7-27 Time: 下午5:14
 * @version $Id$
 */
@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(ShortDate.class)
public class ShortDateHandler extends BaseTypeHandler<ShortDate> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ShortDate parameter, JdbcType jdbcType) throws SQLException {
        try {

            Date date = DateUtils.parseDate(parameter.toString(), "yyyy-MM-dd");
            ps.setTimestamp(i, new Timestamp(date.getTime()));
        } catch (ParseException e) {
            // can't be here.....
            e.printStackTrace();
            ps.setDate(i,null);
        }

    }

    @Override
    public ShortDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return new ShortDate(rs.getDate(columnName));
    }

    @Override
    public ShortDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return new ShortDate(rs.getDate(columnIndex));
    }

    @Override
    public ShortDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return new ShortDate(cs.getDate(columnIndex));
    }
}

package com.lianli.train.mybatis.typehandler;

import com.lianli.train.mybatis.core.ShortDate;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * @author lianli Date: 2020-03-01 Time: 12:20 AM
 * @version Id
 */
@MappedJdbcTypes(JdbcType.TIMESTAMP)
@MappedTypes(ShortDate.class)
public abstract class ShortDateHandler extends BaseTypeHandler<ShortDate> {
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

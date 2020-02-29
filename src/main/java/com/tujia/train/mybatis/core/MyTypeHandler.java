package com.tujia.train.mybatis.core;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author lianli Date: 2019-04-15 Time: 12:35 PM
 * @version $Id$
 */
public class MyTypeHandler implements TypeHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(MyTypeHandler.class);

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {

        logger.info("设置string参数【"+parameter+"】" );
        ps.setString(i,parameter);
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        String result = rs.getString(columnName);
        logger.info("读取string参数1【"+result+"】");
        return result;
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        String result = rs.getString(columnIndex);
        logger.info("读取string参数2【"+result+"】");
        return result;
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        String result = cs.getString(columnIndex);
        logger.info("读取string参数3【"+result+"】");
        return result;
    }

    public static void main(String[] args) {
        String url = "/rba-finance-web/api/finance/account";
        String url1 = "/rba-finance-web/api/finance/accountRegulation/exportBatchReconciliationDetail";
        System.out.print(url.regionMatches(0,url1,0,url.length()));
    }
}

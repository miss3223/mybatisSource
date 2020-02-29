package com.lianli.train.mybatis.mapper.annotation;

import com.lianli.train.mybatis.annotation.MetaMethod;
import com.lianli.train.mybatis.annotation.TableName;
import com.lianli.train.mybatis.mapperProvider.BaseProvider;
import com.lianli.train.mybatis.po.Authors;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * @author lianli Date: 2019-06-03 Time: 9:51 PM
 * @version $Id$
 */
@TableName(value = "authors")
public interface AuthorsMapper extends BaseMapper<Authors> {

    @SelectProvider(type= BaseProvider.class, method= "selectByCondition")
    @MetaMethod(tableName = "authors",order = "id",orderColumn = "desc")
    Authors selectByOrder(@Param("bean") Authors authors);

}

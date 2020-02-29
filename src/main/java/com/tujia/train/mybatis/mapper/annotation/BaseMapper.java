package com.tujia.train.mybatis.mapper.annotation;

import com.tujia.train.mybatis.mapperProvider.BaseProvider;
import java.util.List;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

/**
 * @author lianli Date: 2019-06-02 Time: 12:30 AM
 * @version $Id$
 */
public interface  BaseMapper<T> {

    @SelectProvider(type= BaseProvider.class, method= "selectByCondition")
    T selectByCondition(@Param("bean") Object object);

    @SelectProvider(type= BaseProvider.class, method= "selectByCondition")
    List<T> selectPage(@Param("bean") Object object);

    @InsertProvider(type= BaseProvider.class, method= "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Integer insert(@Param("bean") Object object);

    @UpdateProvider(type= BaseProvider.class, method= "update")
    Integer update(@Param("bean") Object object);

    @DeleteProvider(type= BaseProvider.class, method= "delete")
    Integer delete(@Param("bean") Object object);

}

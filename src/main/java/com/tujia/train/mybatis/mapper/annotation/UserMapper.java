package com.tujia.train.mybatis.mapper.annotation;

import com.tujia.train.mybatis.po.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @author jianhong.li Date: 2017-07-16 Time: 7:21 PM
 * @version $Id$
 */
public interface UserMapper {
    /*sql 在 xml 定义*/
    User queryOne(@Param("id") int id);

    List<User> selectList(Map<String, Object> map);

    /* sql 在注解中定义,参数使用注解 */
    @Select("select user_name as name,age,id , user_status from user where id = #{id}")
    User queryById(@Param("id") int id);

    /* sql 在注解中定义,参数使用 map */
    @Select("select user_name as name,age,id ,user_status from user where id = #{id}")
    User queryByIdWithMapPrameter(Map<String, Object> param);

    Integer insertOne(User user);


}

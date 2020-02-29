package com.tujia.train.mybatis.mapper.annotation;

import com.tujia.train.mybatis.po.Role;
import com.tujia.train.mybatis.po.User;
import org.apache.ibatis.annotations.Param;

/**
 * @author jianhong.li Date: 2017-07-17 Time: 7:01 PM
 * @version $Id$
 */
public interface RoleMapper {
    Role queryOneRole(@Param("user") User user);

}

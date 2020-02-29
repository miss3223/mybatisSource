package com.tujia.train.mybatis.Dao;

import com.tujia.train.mybatis.po.News;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author lianli Date: 2020-01-08 Time: 3:05 PM
 * @version $Id$
 */
public interface NewsDao {

    Integer batchInsertNews(List<News> newsList);

}

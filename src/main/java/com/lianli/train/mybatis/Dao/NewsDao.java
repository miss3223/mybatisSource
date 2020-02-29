package com.lianli.train.mybatis.Dao;

import com.lianli.train.mybatis.po.News;
import java.util.List;

/**
 * @author lianli Date: 2020-01-08 Time: 3:05 PM
 * @version $Id$
 */
public interface NewsDao {

    Integer batchInsertNews(List<News> newsList);

}

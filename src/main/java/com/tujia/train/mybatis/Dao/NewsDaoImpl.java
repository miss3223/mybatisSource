package com.tujia.train.mybatis.Dao;

import com.tujia.train.mybatis.mapper.annotation.NewsMapper;
import com.tujia.train.mybatis.po.News;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author lianli Date: 2020-01-08 Time: 3:05 PM
 * @version $Id$
 */
public class NewsDaoImpl implements NewsDao{

    @Autowired
    private NewsMapper newsMapper;

    @Override
    public Integer batchInsertNews(List<News> newsList) {
        return null;
    }
}

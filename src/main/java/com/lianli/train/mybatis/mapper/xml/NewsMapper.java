package com.lianli.train.mybatis.mapper.xml;

import com.lianli.train.mybatis.po.News;
import com.lianli.train.mybatis.vo.NewsVo;
import java.util.List;
import org.apache.ibatis.annotations.Param;;

/**
 * @author lianli Date: 2019-05-29 Time: 3:57 PM
 * @version $Id$
 */
public interface NewsMapper{

    NewsVo selectAndOrder(@Param("bean") News news);

    List<NewsVo> select(@Param("ids") List<Long> ids);

    List<NewsVo> queryList();

    /**
     * 新增
     * @param news
     *
     */
    int insert(News news);

    /**
     * 更新
     * @param news
     *
     */
    int update(News news);

    /**
     * 删除
     * @param id 主键Id
     *
     */
    void deleteNews(@Param("id") Integer id);


    /**
     * 批量新增
     *
     * @param newsList 参数
     */
    void batchInsertNews(@Param("list") List<News> newsList);





}

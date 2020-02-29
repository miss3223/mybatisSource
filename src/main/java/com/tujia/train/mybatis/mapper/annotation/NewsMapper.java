package com.tujia.train.mybatis.mapper.annotation;


import com.tujia.train.mybatis.annotation.MetaMethod;
import com.tujia.train.mybatis.annotation.TableName;
import com.tujia.train.mybatis.mapperProvider.BaseProvider;
import com.tujia.train.mybatis.po.News;
import com.tujia.train.mybatis.vo.NewsVo;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * @author lianli Date: 2019-05-29 Time: 3:57 PM
 * @version $Id$
 */
@TableName(value = "news")
public interface NewsMapper extends BaseMapper<News> {


    @SelectProvider(type= BaseProvider.class, method= "selectByCondition")
    @MetaMethod(tableName = "news",order = "",orderColumn = "desc",like ={"content","id"})
    NewsVo selectAndOrder(@Param("bean") News news);

    @Select("<script>SELECT id,author_id,title,content,create_time,update_time\n"
        + "FROM news\n"
        + "WHERE (id in<foreach item = 'item' index = 'index' collection = 'ids' open ='('  separator=',' close =')' >"
        + "#{item}</foreach>)</script>")
    List<News> select(@Param("ids") List<Long> ids);


    @Insert("insert into news(author_id,title,content) values(#{authorId},#{title},#{content})")
    int insertDate(News news);


    @Select("select * from news")
    List<NewsVo> queryList();

    @Delete("delete from news where id = #{id}")
    void deleteNews(@Param("id") Integer id);


}

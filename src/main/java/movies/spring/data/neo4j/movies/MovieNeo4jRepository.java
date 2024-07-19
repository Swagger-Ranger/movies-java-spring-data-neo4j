package movies.spring.data.neo4j.movies;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Neo4jRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T>,CrudRepository<T, ID>
 * Neo4jRepository 继承了 PagingAndSortingRepository(PagingAndSortingRepository<T, ID> extends Repository<T, ID>)
 * 来提供基本的 CRUD 操作，并在此基础上增加了分页和排序功能
 * 并又通过 继承 QueryByExampleExecutor 来实现给定的示例实体对象来执行查询，而不需要编写复杂的查询语句。
 * spring data的一些命名约定 @seehttps://springdoc.cn/spring-data-jpa/#repositories.namespace-reference
 *
 * @author liufei
 */
interface MovieNeo4jRepository extends Neo4jRepository<Movie, String> {


    /**
     * MATCH (movie:Movie) WHERE movie.title CONTAINS "a" RETURN movie
     * order by "released"  desc skip 5 limit 10
     * 因为Spring Data在启动时扫描并解析Repository接口中的方法，生成相应的查询逻辑的代理对象。代理对象拦截调用并执行相应的查询逻辑，并使用相应的执行模版类
     * 比如EntityManager、Neo4jTemplate最后交由相应的数据库驱动实际的进行查询。
     * 所以必须在语句中声明 skip 和 limit 和countQuery
     * 只写value的查询语句没有和countQuery 会报错 "Reason: Expected paging query method to have a count query"
     * <p>
     * :#{orderBy(#pageable)}，这个是分页语句，:是 Neo4j 查询语言（Cypher）中的参数占位符，用于动态注入值。
     * #{orderBy(#pageable)} 是一个Spring Expression Language (SpEL)表达式，Spring Data会解析这个SpEL表达式，根据Pageable对象中的排序条件生成适当的ORDER BY子句并将其注入到查询中。
     */
    @Query(value = """
            MATCH (movie:Movie) WHERE movie.title CONTAINS $title RETURN movie
            :#{orderBy(#pageable)} SKIP $skip LIMIT $limit
            """,
            countQuery = """
                    MATCH (movie:Movie) WHERE movie.title CONTAINS $title RETURN
                                count(movie)
                    """)
    Page<Movie> queryExamplePage(@Param("title") String title, Pageable pageable);
}
